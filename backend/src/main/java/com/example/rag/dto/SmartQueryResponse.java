package com.example.rag.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 智能查询响应
 */
@Data
@Builder
public class SmartQueryResponse {
    
    /**
     * 回答内容
     */
    private String answer;
    
    /**
     * 信息来源描述
     */
    private String source;
    
    /**
     * 来源类型
     */
    private SourceType sourceType;
    
    /**
     * 具体的文档来源列表
     */
    private List<String> sources;
    
    /**
     * 附加说明
     */
    private String note;
    
    /**
     * 是否相关（用于内部判断）
     */
    private boolean relevant;
    
    /**
     * 来源类型枚举
     */
    public enum SourceType {
        LIBRARY("图书馆资源"),
        GENERAL("通用知识"),
        ERROR("系统错误");
        
        private final String description;
        
        SourceType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 检查是否相关
     */
    public boolean isRelevant() {
        return relevant;
    }
} 