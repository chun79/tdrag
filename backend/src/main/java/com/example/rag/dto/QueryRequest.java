package com.example.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {
    @JsonProperty("question")
    private String question;
    
    @JsonProperty("message")
    private String message;
    
    private String sessionId;
    
    /**
     * 获取实际的问题内容，优先使用message，其次使用question
     */
    public String getActualQuestion() {
        return message != null ? message : question;
    }
} 