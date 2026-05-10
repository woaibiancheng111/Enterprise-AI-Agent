package com.shixi.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 文档加载器
 *
 * @author shixi
 */
@Component
@Slf4j
public class EnterpriseAppDocumentLoader {

    private final KnowledgeDocumentLoader knowledgeDocumentLoader;

    public EnterpriseAppDocumentLoader(KnowledgeDocumentLoader knowledgeDocumentLoader) {
        this.knowledgeDocumentLoader = knowledgeDocumentLoader;
    }

    /**
     * 加载所有Markdown文档
     *
     * @return 所有Markdown文档
     */
    public List<Document> loadMarkdowns() {
        return knowledgeDocumentLoader.loadAll().stream()
                .flatMap(loadedDocument -> loadedDocument.documents().stream())
                .toList();
    }
}
