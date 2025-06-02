package com.example.rag.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 聊天消息实体类
 */
@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MessageRole role;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "tokens_used")
    private Integer tokensUsed;
    
    @Column(name = "model_used", length = 100)
    private String modelUsed;
    
    @Column(name = "response_time_ms")
    private Long responseTimeMs;
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // 暂时注释掉JSONB字段，等待依赖解决
    // @Type(JsonType.class)
    // @Column(name = "metadata", columnDefinition = "jsonb")
    // private Map<String, Object> metadata;
    
    /**
     * 消息角色枚举
     */
    public enum MessageRole {
        USER,      // 用户消息
        ASSISTANT, // AI助手消息
        SYSTEM     // 系统消息
    }
} 