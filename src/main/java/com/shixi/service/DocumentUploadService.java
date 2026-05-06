package com.shixi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
public class DocumentUploadService {

    @Value("${knowledge.base-path:src/main/resources/documents}")
    private String basePath;

    public String getBasePath() {
        return basePath;
    }

    public List<String> listDocumentFiles() {
        List<String> files = new ArrayList<>();
        Path directory = Paths.get(basePath);

        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                log.error("创建文档目录失败", e);
            }
            return files;
        }

        try (Stream<Path> paths = Files.walk(directory, 1)) {
            paths.filter(Files::isRegularFile)
                 .map(Path::getFileName)
                 .map(Path::toString)
                 .filter(name -> name.toLowerCase().endsWith(".md"))
                 .forEach(files::add);
        } catch (IOException e) {
            log.error("列出文档文件失败", e);
        }

        return files;
    }

    public List<Document> parseAndStoreDocument(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 只支持 MD 文件
        if (!filename.toLowerCase().endsWith(".md")) {
            throw new IllegalArgumentException("目前只支持 MD 格式文件");
        }

        // 保存文件到本地目录
        Path targetPath = Paths.get(basePath, filename);
        Path parent = targetPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        // 如果文件已存在，先删除
        if (Files.exists(targetPath)) {
            Files.delete(targetPath);
        }

        // 保存新文件
        Files.write(targetPath, file.getBytes());
        log.info("文件已保存到: {}", targetPath);

        // 解析文件并返回文档列表
        return parseMarkdownDocument(file);
    }

    private List<Document> parseMarkdownDocument(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(false)
                .withIncludeBlockquote(false)
                .withAdditionalMetadata("filename", filename)
                .withAdditionalMetadata("source", "upload")
                .build();

        Resource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
        List<Document> documents = reader.get();
        log.info("文档 {} 解析成功，共 {} 个块", filename, documents.size());
        return documents;
    }

    public boolean deleteDocument(String filename) {
        try {
            Path filePath = Paths.get(basePath, filename);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("文档已删除: {}", filePath);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.error("删除文档失败: {}", filename, e);
            return false;
        }
    }

    public void reloadVectorStore() {
        log.info("知识库重新加载信号已发送");
    }
}
