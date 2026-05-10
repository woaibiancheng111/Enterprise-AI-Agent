package com.shixi.controller;

import com.shixi.rag.EnterpriseAppVectorStoreConfig;
import com.shixi.rag.service.HybridSearchService;
import com.shixi.security.CurrentUser;
import com.shixi.security.CurrentUserContext;
import com.shixi.service.DocumentUploadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeBaseControllerTest {

    private final DocumentUploadService documentUploadService = mock(DocumentUploadService.class);
    private final EnterpriseAppVectorStoreConfig vectorStoreConfig = mock(EnterpriseAppVectorStoreConfig.class);
    private final HybridSearchService hybridSearchService = mock(HybridSearchService.class);
    private final KnowledgeBaseController controller =
            new KnowledgeBaseController(documentUploadService, vectorStoreConfig, hybridSearchService);

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void uploadAddsDocumentsToVectorStoreAndRefreshesBm25Cache() throws Exception {
        CurrentUserContext.set(new CurrentUser("U004", "admin", null, "系统管理员", "ADMIN"));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "policy.md",
                "text/markdown",
                "# 新制度".getBytes(StandardCharsets.UTF_8)
        );
        List<Document> documents = List.of(new Document("请假制度"));
        when(documentUploadService.parseAndStoreDocument(file)).thenReturn(documents);

        ResponseEntity<Map<String, Object>> response = controller.uploadDocument(file);

        assertTrue(Boolean.TRUE.equals(response.getBody().get("success")));
        assertEquals("文档上传成功，索引已更新", response.getBody().get("message"));
        verify(vectorStoreConfig).addDocuments(documents);
        verify(hybridSearchService).clearCache();
    }

    @Test
    void reloadRebuildsVectorStoreAndRefreshesBm25Cache() {
        CurrentUserContext.set(new CurrentUser("U004", "admin", null, "系统管理员", "ADMIN"));

        ResponseEntity<Map<String, Object>> response = controller.reloadKnowledgeBase();

        assertTrue(Boolean.TRUE.equals(response.getBody().get("success")));
        verify(vectorStoreConfig).clearAndReload();
        verify(hybridSearchService).clearCache();
    }
}
