package com.shixi.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Disabled("Manual smoke test; Spring context loads external AI and database infrastructure.")
class EnterpriseAppDocumentLoaderTest {

    @Resource
    EnterpriseAppDocumentLoader enterpriseAppDocumentLoader;
    @Test
    void loadMarkdowns() {
        enterpriseAppDocumentLoader.loadMarkdowns();
    }
}
