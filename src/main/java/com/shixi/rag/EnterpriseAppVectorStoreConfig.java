package com.shixi.rag;

import com.shixi.service.DocumentUploadService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Configuration
@Slf4j
public class EnterpriseAppVectorStoreConfig {

    private final ResourcePatternResolver resourcePatternResolver;
    private final DocumentUploadService documentUploadService;
    private final EmbeddingModel embeddingModel;

    private SimpleVectorStore vectorStore;
    private final ReentrantLock reloadLock = new ReentrantLock();
    private final ConcurrentHashMap<String, Boolean> documentTracking = new ConcurrentHashMap<>();

    public EnterpriseAppVectorStoreConfig(
            ResourcePatternResolver resourcePatternResolver,
            DocumentUploadService documentUploadService,
            EmbeddingModel embeddingModel) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.documentUploadService = documentUploadService;
        this.embeddingModel = embeddingModel;
    }

    @PostConstruct
    public void init() {
        this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        loadDocuments();
    }

    @Bean
    VectorStore enterpriseAppVectorStore() {
        return this.vectorStore;
    }

    public void loadDocuments() {
        reloadLock.lock();
        try {
            List<Document> allDocuments = new ArrayList<>();

            // 加载 classpath 中的文档
            try {
                Resource[] resources = resourcePatternResolver.getResources("classpath:documents/*.md");
                for (Resource resource : resources) {
                    String filename = resource.getFilename();
                    if (filename == null) continue;

                    log.info("加载文档: {}", filename);

                    MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                            .withHorizontalRuleCreateDocument(true)
                            .withIncludeCodeBlock(false)
                            .withIncludeBlockquote(false)
                            .withAdditionalMetadata("filename", filename)
                            .withAdditionalMetadata("source", "classpath")
                            .build();

                    MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                    List<Document> docs = reader.get();
                    allDocuments.addAll(docs);
                    documentTracking.put(filename, true);
                    log.info("文档 {} 加载成功，共 {} 个块", filename, docs.size());
                }
            } catch (Exception e) {
                log.warn("加载 classpath 文档失败", e);
            }

            // 加载上传的文档
            List<String> uploadedFiles = documentUploadService.listDocumentFiles();
            for (String filename : uploadedFiles) {
                if (documentTracking.containsKey(filename)) {
                    continue;
                }

                try {
                    String lowerFilename = filename.toLowerCase();
                    if (lowerFilename.endsWith(".md")) {
                        String basePath = documentUploadService.getBasePath();
                        Path filePath = Paths.get(basePath, filename);
                        if (Files.exists(filePath)) {
                            Resource fileResource = new FileSystemResource(filePath.toFile());
                            MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                                    .withHorizontalRuleCreateDocument(true)
                                    .withIncludeCodeBlock(false)
                                    .withIncludeBlockquote(false)
                                    .withAdditionalMetadata("filename", filename)
                                    .withAdditionalMetadata("source", "upload")
                                    .build();

                            MarkdownDocumentReader reader = new MarkdownDocumentReader(fileResource, config);
                            List<Document> docs = reader.get();
                            allDocuments.addAll(docs);
                            documentTracking.put(filename, true);
                            log.info("上传文档 {} 加载成功，共 {} 个块", filename, docs.size());
                        }
                    }
                } catch (Exception e) {
                    log.warn("加载上传文档失败: {}", filename, e);
                }
            }

            // 添加到向量存储
            if (!allDocuments.isEmpty()) {
                vectorStore.add(allDocuments);
                log.info("知识库初始化完成，共加载 {} 个文档块", allDocuments.size());
            }

        } finally {
            reloadLock.unlock();
        }
    }

    public void addDocuments(List<Document> documents) {
        if (documents != null && !documents.isEmpty()) {
            vectorStore.add(documents);
            log.info("新增 {} 个文档块到知识库", documents.size());
        }
    }

    public void clearAndReload() {
        reloadLock.lock();
        try {
            this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();
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
}
