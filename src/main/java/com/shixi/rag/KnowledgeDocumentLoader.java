package com.shixi.rag;

import com.shixi.service.DocumentUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class KnowledgeDocumentLoader {

    public static final String METADATA_FILENAME = "filename";
    public static final String METADATA_SOURCE = "source";
    public static final String METADATA_CHUNK_INDEX = "chunkIndex";

    private static final String CLASSPATH_SOURCE = "classpath";
    private static final String UPLOAD_SOURCE = "upload";

    private final ResourcePatternResolver resourcePatternResolver;
    private final DocumentUploadService documentUploadService;

    public KnowledgeDocumentLoader(ResourcePatternResolver resourcePatternResolver,
                                   DocumentUploadService documentUploadService) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.documentUploadService = documentUploadService;
    }

    public List<LoadedKnowledgeDocument> loadAll() {
        List<LoadedKnowledgeDocument> loadedDocuments = new ArrayList<>();
        loadedDocuments.addAll(loadClasspathDocuments());
        loadedDocuments.addAll(loadUploadedDocuments());
        return loadedDocuments;
    }

    public List<Document> prepareDocuments(List<Document> documents) {
        String filename = resolveFilename(documents);
        String source = resolveSource(documents);
        return prepareDocuments(filename, source, documents);
    }

    private List<LoadedKnowledgeDocument> loadClasspathDocuments() {
        List<LoadedKnowledgeDocument> loadedDocuments = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:documents/*.md");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null || filename.isBlank()) {
                    continue;
                }
                List<Document> documents = readMarkdown(resource, filename, CLASSPATH_SOURCE);
                loadedDocuments.add(new LoadedKnowledgeDocument(filename, CLASSPATH_SOURCE, documents));
                log.info("文档 {} 加载成功，共 {} 个块", filename, documents.size());
            }
        } catch (Exception e) {
            log.warn("加载 classpath 文档失败", e);
        }
        return loadedDocuments;
    }

    private List<LoadedKnowledgeDocument> loadUploadedDocuments() {
        List<LoadedKnowledgeDocument> loadedDocuments = new ArrayList<>();
        for (String filename : documentUploadService.listDocumentFiles()) {
            if (filename == null || !filename.toLowerCase().endsWith(".md")) {
                continue;
            }

            try {
                Path filePath = Paths.get(documentUploadService.getBasePath(), filename);
                if (!Files.exists(filePath)) {
                    continue;
                }
                Resource resource = new FileSystemResource(filePath.toFile());
                List<Document> documents = readMarkdown(resource, filename, UPLOAD_SOURCE);
                loadedDocuments.add(new LoadedKnowledgeDocument(filename, UPLOAD_SOURCE, documents));
                log.info("上传文档 {} 加载成功，共 {} 个块", filename, documents.size());
            } catch (Exception e) {
                log.warn("加载上传文档失败: {}", filename, e);
            }
        }
        return loadedDocuments;
    }

    private List<Document> readMarkdown(Resource resource, String filename, String source) {
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(false)
                .withIncludeBlockquote(false)
                .withAdditionalMetadata(METADATA_FILENAME, filename)
                .withAdditionalMetadata(METADATA_SOURCE, source)
                .build();

        MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
        return prepareDocuments(filename, source, reader.get());
    }

    private List<Document> prepareDocuments(String filename, String source, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        String resolvedFilename = hasText(filename) ? filename : resolveFilename(documents);
        String resolvedSource = hasText(source) ? source : resolveSource(documents);

        List<Document> preparedDocuments = new ArrayList<>(documents.size());
        for (int index = 0; index < documents.size(); index++) {
            Document document = documents.get(index);
            Map<String, Object> metadata = new LinkedHashMap<>(document.getMetadata());
            if (hasText(resolvedFilename)) {
                metadata.put(METADATA_FILENAME, resolvedFilename);
            }
            if (hasText(resolvedSource)) {
                metadata.put(METADATA_SOURCE, resolvedSource);
            }
            metadata.put(METADATA_CHUNK_INDEX, index);

            preparedDocuments.add(Document.builder()
                    .id(stableId(resolvedSource, resolvedFilename, index, document.getText()))
                    .text(document.getText())
                    .metadata(metadata)
                    .build());
        }
        return preparedDocuments;
    }

    public static String resolveFilename(List<Document> documents) {
        return resolveMetadata(documents, METADATA_FILENAME);
    }

    public static String resolveSource(List<Document> documents) {
        String source = resolveMetadata(documents, METADATA_SOURCE);
        return hasText(source) ? source : UPLOAD_SOURCE;
    }

    private static String resolveMetadata(List<Document> documents, String key) {
        if (documents == null) {
            return null;
        }
        return documents.stream()
                .map(Document::getMetadata)
                .map(metadata -> metadata.get(key))
                .filter(value -> value != null && hasText(String.valueOf(value)))
                .map(String::valueOf)
                .findFirst()
                .orElse(null);
    }

    private static String stableId(String source, String filename, int index, String content) {
        String input = String.join(":", safePart(source), safePart(filename), String.valueOf(index), safePart(content));
        return uuidFromSha256(input);
    }

    private static String uuidFromSha256(String input) {
        byte[] hash = sha256(input);
        hash[6] = (byte) ((hash[6] & 0x0f) | 0x50);
        hash[8] = (byte) ((hash[8] & 0x3f) | 0x80);
        String hex = HexFormat.of().formatHex(hash, 0, 16);
        return hex.substring(0, 8) + "-"
                + hex.substring(8, 12) + "-"
                + hex.substring(12, 16) + "-"
                + hex.substring(16, 20) + "-"
                + hex.substring(20, 32);
    }

    private static byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String safePart(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record LoadedKnowledgeDocument(String filename, String source, List<Document> documents) {
    }
}
