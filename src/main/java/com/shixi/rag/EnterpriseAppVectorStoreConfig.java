package com.shixi.rag;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class EnterpriseAppVectorStoreConfig {

    private static final int VECTOR_STORE_ADD_BATCH_SIZE = 25;

    private final VectorStore vectorStore;
    private final KnowledgeDocumentLoader documentLoader;

    private final ReentrantLock reloadLock = new ReentrantLock();
    private final ConcurrentHashMap<String, List<String>> documentTracking = new ConcurrentHashMap<>();

    public EnterpriseAppVectorStoreConfig(
            VectorStore vectorStore,
            KnowledgeDocumentLoader documentLoader) {
        this.vectorStore = vectorStore;
        this.documentLoader = documentLoader;
    }

    @PostConstruct
    public void init() {
        loadDocuments();
    }

    public void loadDocuments() {
        reloadLock.lock();
        try {
            List<Document> allDocuments = new ArrayList<>();
            for (KnowledgeDocumentLoader.LoadedKnowledgeDocument loadedDocument : documentLoader.loadAll()) {
                String documentKey = documentKey(loadedDocument.source(), loadedDocument.filename());
                if (documentTracking.containsKey(documentKey)) {
                    continue;
                }
                allDocuments.addAll(loadedDocument.documents());
                trackDocumentIds(loadedDocument.source(), loadedDocument.filename(), loadedDocument.documents());
            }

            if (!allDocuments.isEmpty()) {
                deleteManagedDocumentsFromVectorStore();
                addToVectorStore(allDocuments);
                log.info("知识库初始化完成，共加载 {} 个文档块", allDocuments.size());
            }
        } finally {
            reloadLock.unlock();
        }
    }

    public void addDocuments(List<Document> documents) {
        List<Document> preparedDocuments = documentLoader.prepareDocuments(documents);
        if (!preparedDocuments.isEmpty()) {
            String filename = KnowledgeDocumentLoader.resolveFilename(preparedDocuments);
            String source = KnowledgeDocumentLoader.resolveSource(preparedDocuments);
            if (filename != null) {
                deleteDocumentFromVectorStore(source, filename);
            }
            addToVectorStore(preparedDocuments);
            if (filename != null) {
                trackDocumentIds(source, filename, preparedDocuments);
            }
            log.info("新增 {} 个文档块到知识库", preparedDocuments.size());
        }
    }

    public void deleteDocumentFromVectorStore(String filename) {
        deleteDocumentFromVectorStore("upload", filename);
    }

    public void deleteDocumentFromVectorStore(String source, String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }
        List<String> documentIds = documentTracking.remove(documentKey(source, filename));
        if (documentIds != null && !documentIds.isEmpty()) {
            vectorStore.delete(documentIds);
            log.info("已从向量索引删除文档 {} 的 {} 个文档块", filename, documentIds.size());
        }
        deleteByMetadata(source, filename);
        log.info("已按元数据从向量索引删除文档 {}", filename);
    }

    public void clearAndReload() {
        reloadLock.lock();
        try {
            deleteManagedDocumentsFromVectorStore();
            documentTracking.clear();
            loadDocuments();
            log.info("知识库已重新加载");
        } finally {
            reloadLock.unlock();
        }
    }

    public int getDocumentCount() {
        return documentTracking.size();
    }

    public List<String> getLoadedDocuments() {
        return new ArrayList<>(documentTracking.keySet());
    }

    private void deleteManagedDocumentsFromVectorStore() {
        List<String> allDocumentIds = documentTracking.values().stream()
                .flatMap(List::stream)
                .toList();
        if (!allDocumentIds.isEmpty()) {
            vectorStore.delete(allDocumentIds);
        }
        deleteBySource("classpath");
        deleteBySource("upload");
    }

    private void deleteBySource(String source) {
        vectorStore.delete(new FilterExpressionBuilder()
                .eq(KnowledgeDocumentLoader.METADATA_SOURCE, source)
                .build());
    }

    private void deleteByMetadata(String source, String filename) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        vectorStore.delete(builder.and(
                builder.eq(KnowledgeDocumentLoader.METADATA_SOURCE, source),
                builder.eq(KnowledgeDocumentLoader.METADATA_FILENAME, filename)
        ).build());
    }

    private void trackDocumentIds(String source, String filename, List<Document> documents) {
        documentTracking.put(documentKey(source, filename), documents.stream()
                .map(Document::getId)
                .collect(Collectors.toList()));
    }

    private void addToVectorStore(List<Document> documents) {
        for (int fromIndex = 0; fromIndex < documents.size(); fromIndex += VECTOR_STORE_ADD_BATCH_SIZE) {
            int toIndex = Math.min(fromIndex + VECTOR_STORE_ADD_BATCH_SIZE, documents.size());
            vectorStore.add(documents.subList(fromIndex, toIndex));
        }
    }

    private String documentKey(String source, String filename) {
        return source + ":" + filename;
    }
}
