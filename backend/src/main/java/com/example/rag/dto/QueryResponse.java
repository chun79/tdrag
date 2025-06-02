package com.example.rag.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.List;

/**
 * 查询响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryResponse {
    
    /**
     * 回答内容
     */
    private String answer;
    
    /**
     * 文档来源列表
     */
    private List<String> sources;
    
    /**
     * 来源类型描述
     */
    private String sourceType;
    
    /**
     * 附加说明
     */
    private String note;
    
    /**
     * 兼容性构造函数
     */
    public QueryResponse(String answer) {
        this.answer = answer;
    }
} 