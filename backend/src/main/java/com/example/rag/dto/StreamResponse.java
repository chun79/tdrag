package com.example.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 流式响应数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamResponse {
    
    /**
     * 响应类型
     */
    public enum Type {
        START,          // 开始响应
        THINKING,       // 思考过程
        ANSWER_START,   // 正式回答开始
        CHUNK,          // 内容块
        SOURCE,         // 来源信息
        NOTE,           // 附加说明
        END,            // 结束响应
        ERROR           // 错误信息
    }
    
    /**
     * 响应类型
     */
    private Type type;
    
    /**
     * 内容
     */
    private String content;
    
    /**
     * 来源类型
     */
    private String sourceType;
    
    /**
     * 参考文档列表
     */
    private List<String> sources;
    
    /**
     * 附加说明
     */
    private String note;
    
    /**
     * 是否完成
     */
    private boolean done;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 创建开始响应
     */
    public static StreamResponse start(String sourceType) {
        return StreamResponse.builder()
                .type(Type.START)
                .sourceType(sourceType)
                .done(false)
                .build();
    }
    
    /**
     * 创建思考过程响应
     */
    public static StreamResponse thinking(String content) {
        return StreamResponse.builder()
                .type(Type.THINKING)
                .content(content)
                .done(false)
                .build();
    }
    
    /**
     * 创建正式回答开始响应
     */
    public static StreamResponse answerStart() {
        return StreamResponse.builder()
                .type(Type.ANSWER_START)
                .done(false)
                .build();
    }
    
    /**
     * 创建内容块响应
     */
    public static StreamResponse chunk(String content) {
        return StreamResponse.builder()
                .type(Type.CHUNK)
                .content(content)
                .done(false)
                .build();
    }
    
    /**
     * 创建来源信息响应
     */
    public static StreamResponse source(List<String> sources) {
        return StreamResponse.builder()
                .type(Type.SOURCE)
                .sources(sources)
                .done(false)
                .build();
    }
    
    /**
     * 创建附加说明响应
     */
    public static StreamResponse note(String note) {
        return StreamResponse.builder()
                .type(Type.NOTE)
                .note(note)
                .done(false)
                .build();
    }
    
    /**
     * 创建结束响应
     */
    public static StreamResponse end() {
        return StreamResponse.builder()
                .type(Type.END)
                .done(true)
                .build();
    }
    
    /**
     * 创建错误响应
     */
    public static StreamResponse error(String error) {
        return StreamResponse.builder()
                .type(Type.ERROR)
                .error(error)
                .done(true)
                .build();
    }
} 