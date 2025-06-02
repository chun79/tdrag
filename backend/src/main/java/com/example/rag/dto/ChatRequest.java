package com.example.rag.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 聊天请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {
    
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 4000, message = "消息内容不能超过4000字符")
    private String message;
    
    private String sessionId;
    
    @Builder.Default
    private Boolean useRag = true;
    
    private String category;
    
    @Builder.Default
    private Double temperature = 0.7;
    
    @Builder.Default
    private Integer maxTokens = 2048;
    
    private String model;
} 