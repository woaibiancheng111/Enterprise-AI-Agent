package com.shixi.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EnterpriseAppDocumentLoaderTest {

    @Resource
    EnterpriseAppDocumentLoader enterpriseAppDocumentLoader;
    @Test
    void loadMarkdowns() {
        enterpriseAppDocumentLoader.loadMarkdowns();
    }
}