package com.example.rag.repository;

import com.example.rag.model.DocumentChunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档块仓储接口 - Elasticsearch
 */
@Repository
public interface DocumentChunkRepository extends ElasticsearchRepository<DocumentChunk, String> {
    
    /**
     * 根据文档ID查找所有块
     */
    List<DocumentChunk> findByDocumentId(String documentId);
    
    /**
     * 根据文档ID查找所有块（分页）
     */
    Page<DocumentChunk> findByDocumentId(String documentId, Pageable pageable);
    
    /**
     * 根据分类查找文档块
     */
    Page<DocumentChunk> findByCategory(String category, Pageable pageable);
    
    /**
     * 根据文档ID和块索引查找
     */
    DocumentChunk findByDocumentIdAndChunkIndex(String documentId, Integer chunkIndex);
    
    /**
     * 根据内容进行全文搜索
     */
    Page<DocumentChunk> findByContentContaining(String content, Pageable pageable);
    
    /**
     * 统计文档的块数量
     */
    long countByDocumentId(String documentId);
    
    /**
     * 删除文档的所有块
     */
    void deleteByDocumentId(String documentId);
    
    /**
     * 根据文档ID列表查找块
     */
    List<DocumentChunk> findByDocumentIdIn(List<String> documentIds);
} 