package com.example.rag.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文档块实体类 - 存储在Elasticsearch中
 */
@Document(indexName = "document_chunks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {
    
    @Id
    private String id;
    
    @Field(type = FieldType.Keyword)
    private String documentId;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;
    
    @Field(type = FieldType.Dense_Vector, dims = 768)
    private List<Float> embedding;
    
    @Field(type = FieldType.Integer)
    private Integer chunkIndex;
    
    @Field(type = FieldType.Integer)
    private Integer startPosition;
    
    @Field(type = FieldType.Integer)
    private Integer endPosition;
    
    @Field(type = FieldType.Keyword)
    private String category;
    
    @Field(type = FieldType.Object)
    private Map<String, Object> metadata;
    
    @Field(type = FieldType.Keyword)
    private String createdAt;
    
    @Field(type = FieldType.Keyword)
    private String updatedAt;
} 