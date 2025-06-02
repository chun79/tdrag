package com.example.rag.service;

import com.example.rag.model.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 向量搜索服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorSearchService {
    
    private final VectorStore vectorStore;
    
    /**
     * 向量化搜索文档
     */
    public List<DocumentChunk> vectorSearch(String query, int topK) {
        try {
            log.info("开始向量搜索，查询: {}, topK: {}", query, topK);
            
            // 限制topK的最大值以避免num_candidates错误
            int limitedTopK = Math.min(topK, 1000);
            if (topK != limitedTopK) {
                log.warn("topK值 {} 超过限制，调整为 {}", topK, limitedTopK);
            }
            
            // 使用Spring AI VectorStore进行相似性搜索
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(limitedTopK)
                    .build();
            List<Document> documents = vectorStore.similaritySearch(searchRequest);
            
            log.info("向量搜索返回 {} 个结果", documents.size());
            
            // 转换为DocumentChunk
            return documents.stream()
                    .map(this::convertToDocumentChunk)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("向量搜索失败", e);
            return List.of();
        }
    }
    
    /**
     * 添加文档到向量存储
     */
    public void addDocument(String content, Map<String, Object> metadata) {
        try {
            log.info("添加文档到向量存储，内容长度: {}", content.length());
            
            Document document = new Document(content, metadata);
            vectorStore.add(List.of(document));
            
            log.info("文档已添加到向量存储");
            
        } catch (Exception e) {
            log.error("添加文档到向量存储失败", e);
        }
    }
    
    /**
     * 批量添加文档到向量存储
     */
    public void addDocumentsToVectorStore(List<DocumentChunk> chunks) {
        try {
            log.info("批量添加 {} 个文档块到向量存储", chunks.size());
            
            List<Document> documents = chunks.stream()
                    .map(chunk -> {
                        Document document = new Document(chunk.getContent());
                        Map<String, Object> metadata = document.getMetadata();
                        metadata.put("id", chunk.getId());
                        metadata.put("documentId", chunk.getDocumentId());
                        metadata.put("chunkIndex", chunk.getChunkIndex());
                        metadata.put("category", chunk.getCategory());
                        return document;
                    })
                    .collect(Collectors.toList());
            
            vectorStore.add(documents);
            
            log.info("成功批量添加 {} 个文档块到向量存储", chunks.size());
            
        } catch (Exception e) {
            log.error("批量添加文档块到向量存储失败", e);
        }
    }
    
    /**
     * 从向量存储删除文档
     */
    public void deleteDocumentFromVectorStore(String documentId) {
        try {
            log.info("从向量存储删除文档: {}", documentId);
            
            // 暂时跳过向量删除，避免搜索限制问题
            // TODO: 需要找到不依赖搜索的删除方法
            log.warn("向量存储删除暂时跳过，避免Elasticsearch限制问题: {}", documentId);
            
        } catch (Exception e) {
            log.error("从向量存储删除文档失败: {}", documentId, e);
            throw new RuntimeException("从向量存储删除文档失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据documentId查找向量存储中的文档
     */
    public List<Document> findDocumentsByDocumentId(String documentId) {
        try {
            log.debug("在向量存储中查找documentId: {}", documentId);
            
            // 暂时返回空列表，避免num_candidates错误
            // TODO: 需要找到更好的方法来查找特定documentId的向量文档
            log.warn("向量文档查找暂时禁用，避免Elasticsearch限制问题");
            return List.of();
            
        } catch (Exception e) {
            log.error("查找向量文档失败: {}", documentId, e);
            return List.of();
        }
    }
    
    /**
     * 批量删除向量文档
     */
    public void batchDeleteFromVectorStore(List<String> documentIds) {
        try {
            log.info("批量删除向量存储中的文档: {}", documentIds);
            
            for (String documentId : documentIds) {
                try {
                    deleteDocumentFromVectorStore(documentId);
                } catch (Exception e) {
                    log.error("删除文档 {} 失败，继续处理其他文档", documentId, e);
                }
            }
            
            log.info("批量删除完成");
            
        } catch (Exception e) {
            log.error("批量删除向量文档失败", e);
        }
    }
    
    /**
     * 将Spring AI Document转换为DocumentChunk
     */
    private DocumentChunk convertToDocumentChunk(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        log.info("向量存储Document metadata: {}", metadata);
        
        String documentId = (String) metadata.get("documentId");
        log.info("提取的documentId: {}", documentId);
        
        return DocumentChunk.builder()
                .id((String) metadata.get("id"))
                .documentId(documentId)
                .content(document.getText())
                .chunkIndex((Integer) metadata.get("chunkIndex"))
                .category((String) metadata.get("category"))
                .build();
    }
} 