package com.example.rag.controller;

import com.example.rag.dto.DocumentUploadRequest;
import com.example.rag.model.Document;
import com.example.rag.model.DocumentChunk;
import com.example.rag.model.User;
import com.example.rag.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * 文档控制器
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DocumentController {
    
    private final DocumentService documentService;
    
    /**
     * 上传文档
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "general") String category,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "autoProcess", defaultValue = "true") Boolean autoProcess) {
        
        try {
            User user = getCurrentUser();
            
            DocumentUploadRequest request = DocumentUploadRequest.builder()
                    .file(file)
                    .category(category)
                    .description(description)
                    .autoProcess(autoProcess)
                    .build();
            
            Document document = documentService.uploadDocument(request, user);
            return ResponseEntity.ok(document);
            
        } catch (Exception e) {
            log.error("文档上传失败", e);
            return ResponseEntity.badRequest().body("文档上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户文档列表
     */
    @GetMapping("/list")
    public ResponseEntity<Page<Document>> getUserDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            User user = getCurrentUser();
            Pageable pageable = PageRequest.of(page, size);
            Page<Document> documents = documentService.getUserDocuments(user, pageable);
            return ResponseEntity.ok(documents);
            
        } catch (Exception e) {
            log.error("获取文档列表失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取文档详情
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<Document> getDocument(@PathVariable String documentId) {
        try {
            Optional<Document> document = documentService.getDocumentById(documentId);
            return document.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
            
        } catch (Exception e) {
            log.error("获取文档详情失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 删除文档
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String documentId) {
        try {
            User user = getCurrentUser();
            documentService.deleteDocument(documentId, user);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("删除文档失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取文档块
     */
    @GetMapping("/{documentId}/chunks")
    public ResponseEntity<List<DocumentChunk>> getDocumentChunks(@PathVariable String documentId) {
        try {
            List<DocumentChunk> chunks = documentService.getDocumentChunks(documentId);
            return ResponseEntity.ok(chunks);
            
        } catch (Exception e) {
            log.error("获取文档块失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取当前用户（临时实现）
     */
    private User getCurrentUser() {
        // TODO: 从Spring Security上下文获取当前用户
        // 暂时返回模拟用户，使用固定ID与ChatController保持一致
        return User.builder()
                .id(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .username("testuser")
                .email("test@example.com")
                .role(User.UserRole.USER)
                .build();
    }
} 