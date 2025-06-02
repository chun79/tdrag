package com.example.rag.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文档实体类
 */
@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Document {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "document_id", unique = true, nullable = false, length = 255)
    private String documentId;
    
    @Column(name = "filename", nullable = false, length = 255)
    private String filename;
    
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(name = "content_type", length = 100)
    private String contentType;
    
    @Column(name = "category", length = 50)
    @Builder.Default
    private String category = "general";
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "passwordHash"})
    private User uploadUser;
    
    @Column(name = "upload_time")
    @Builder.Default
    private LocalDateTime uploadTime = LocalDateTime.now();
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PROCESSING;
    
    @Column(name = "chunks_count")
    @Builder.Default
    private Integer chunksCount = 0;
    
    // 暂时注释掉JSONB字段，等待依赖解决
    // @Type(JsonType.class)
    // @Column(name = "metadata", columnDefinition = "jsonb")
    // private Map<String, Object> metadata;
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    /**
     * 文档状态枚举
     */
    public enum DocumentStatus {
        PROCESSING,    // 处理中
        COMPLETED,     // 已完成
        FAILED,        // 处理失败
        DELETED        // 已删除
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
} 