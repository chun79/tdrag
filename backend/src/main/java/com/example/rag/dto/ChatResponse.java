package com.example.rag.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
    
    private String sessionId;
    
    private String messageId;
    
    private String response;
    
    private List<String> sources;
    
    private Integer tokensUsed;
    
    private String modelUsed;
    
    private Long responseTimeMs;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    private Boolean success;
    
    private String error;
} 