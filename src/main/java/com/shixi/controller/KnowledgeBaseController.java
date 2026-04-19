package com.shixi.controller;

import com.shixi.service.DocumentUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("knowledge")
@Slf4j
public class KnowledgeBaseController {

    private final DocumentUploadService documentUploadService;

    public KnowledgeBaseController(DocumentUploadService documentUploadService) {
        this.documentUploadService = documentUploadService;
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listDocuments() {
        List<String> documents = documentUploadService.listDocumentFiles();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("documents", documents);
        result.put("count", documents.size());
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file) {
        log.info("收到文件上传请求: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());
        
        Map<String, Object> result = new HashMap<>();
        try {
            // 验证文件类型
            String filename = file.getOriginalFilename();
            if (filename == null || filename.isBlank()) {
                result.put("success", false);
                result.put("message", "文件名不能为空");
                return ResponseEntity.badRequest().body(result);
            }

            String extension = getFileExtension(filename).toLowerCase();
            if (!isSupportedFileType(extension)) {
                result.put("success", false);
                result.put("message", "不支持的文件类型: " + extension + "，目前仅支持 MD 格式");
                return ResponseEntity.badRequest().body(result);
            }

            // 验证文件大小（最大 10MB）
            if (file.getSize() > 10 * 1024 * 1024) {
                result.put("success", false);
                result.put("message", "文件大小不能超过 10MB");
                return ResponseEntity.badRequest().body(result);
            }

            // 上传并解析文档
            List<Document> documents = documentUploadService.parseAndStoreDocument(file);
            
            result.put("success", true);
            result.put("message", "文档上传成功");
            result.put("filename", filename);
            result.put("chunks", documents.size());
            
            log.info("文档 {} 上传成功，解析出 {} 个文档块", filename, documents.size());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("文档上传失败", e);
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @DeleteMapping("/file/{filename}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String filename) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean deleted = documentUploadService.deleteDocument(filename);
            result.put("success", deleted);
            result.put("message", deleted ? "删除成功" : "删除失败，文件不存在");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("文档删除失败", e);
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reloadKnowledgeBase() {
        Map<String, Object> result = new HashMap<>();
        try {
            documentUploadService.reloadVectorStore();
            result.put("success", true);
            result.put("message", "知识库重新加载成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("知识库重新加载失败", e);
            result.put("success", false);
            result.put("message", "重新加载失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

    private boolean isSupportedFileType(String extension) {
        return extension.equals("md");
    }
}
