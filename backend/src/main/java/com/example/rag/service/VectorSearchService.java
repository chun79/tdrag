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
            
            // 转换为DocumentChunk并过滤无用内容
            List<DocumentChunk> chunks = documents.stream()
                    .map(this::convertToDocumentChunk)
                    .filter(this::isUsefulContent)  // 添加内容过滤
                    .collect(Collectors.toList());
            
            log.info("过滤后剩余 {} 个有用的文档块", chunks.size());
            
            // 如果过滤后结果太少，尝试获取更多结果
            if (chunks.size() < topK && documents.size() == limitedTopK) {
                log.info("过滤后结果不足，尝试获取更多文档块");
                SearchRequest expandedRequest = SearchRequest.builder()
                        .query(query)
                        .topK(Math.min(limitedTopK * 2, 1000))
                        .build();
                List<Document> expandedDocuments = vectorStore.similaritySearch(expandedRequest);
                
                chunks = expandedDocuments.stream()
                        .map(this::convertToDocumentChunk)
                        .filter(this::isUsefulContent)
                        .limit(topK)  // 限制最终结果数量
                        .collect(Collectors.toList());
                
                log.info("扩展搜索后过滤得到 {} 个有用的文档块", chunks.size());
            }
            
            return chunks;
                    
        } catch (Exception e) {
            log.error("向量搜索失败", e);
            return List.of();
        }
    }
    
    /**
     * 向量化搜索文档（带相似度阈值）
     */
    public List<DocumentChunk> vectorSearchWithThreshold(String query, int topK, double similarityThreshold) {
        try {
            log.info("开始向量搜索（带阈值），查询: {}, topK: {}, 阈值: {}", query, topK, similarityThreshold);
            
            // 限制topK的最大值以避免num_candidates错误
            int limitedTopK = Math.min(topK, 1000);
            if (topK != limitedTopK) {
                log.warn("topK值 {} 超过限制，调整为 {}", topK, limitedTopK);
            }
            
            // 使用Spring AI VectorStore进行相似性搜索，设置相似度阈值
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(limitedTopK)
                    .similarityThreshold(similarityThreshold) // 关键：设置相似度阈值
                    .build();
            List<Document> documents = vectorStore.similaritySearch(searchRequest);
            
            log.info("向量搜索（阈值: {}）返回 {} 个结果", similarityThreshold, documents.size());
            
            // 如果没有找到满足阈值的文档，这本身就说明文档不相关
            if (documents.isEmpty()) {
                log.info("❌ 没有找到相似度 >= {} 的文档，判定为不相关", similarityThreshold);
                return List.of();
            }
            
            // 转换为DocumentChunk并过滤无用内容
            List<DocumentChunk> chunks = documents.stream()
                    .map(this::convertToDocumentChunk)
                    .filter(this::isUsefulContent)  // 添加内容过滤
                    .collect(Collectors.toList());
            
            log.info("过滤后剩余 {} 个有用的文档块", chunks.size());
            
            return chunks;
                    
        } catch (Exception e) {
            log.error("向量搜索（带阈值）失败", e);
            return List.of();
        }
    }
    
    /**
     * 判断文档块内容是否有用
     */
    private boolean isUsefulContent(DocumentChunk chunk) {
        if (chunk == null || chunk.getContent() == null) {
            return false;
        }
        
        String content = chunk.getContent().trim();
        
        // 内容太短，可能没有实际价值
        if (content.length() < 30) {
            log.debug("过滤掉内容过短的文档块: {}", content.substring(0, Math.min(content.length(), 30)));
            return false;
        }
        
        // 过滤明显的版权声明和法律声明
        List<String> uselessPatterns = List.of(
            "侵权必究", "版权所有", "保留所有权利", "未经授权", "不得进行传播",
            "维权措施", "法律责任", "著作权合同", "广告经营许可证"
        );
        
        boolean hasUselessPattern = uselessPatterns.stream()
                .anyMatch(pattern -> content.contains(pattern));
        
        if (hasUselessPattern) {
            log.debug("过滤掉包含无用模式的文档块: {}", content.substring(0, Math.min(content.length(), 50)));
            return false;
        }
        
        // 过滤明显的目录内容（包含大量页码和点号）
        long dotCount = content.chars().filter(ch -> ch == '.').count();
        long digitCount = content.chars().filter(Character::isDigit).count();
        if (dotCount > 20 && digitCount > 50 && content.length() < 200) {
            log.debug("过滤掉疑似目录的文档块: {}", content.substring(0, Math.min(content.length(), 50)));
            return false;
        }
        
        // 过滤字母比例过低的内容（可能是乱码或格式化内容）
        long letterCount = content.chars().filter(Character::isLetter).count();
        if (letterCount < content.length() * 0.2 && content.length() > 100) {
            log.debug("过滤掉字母比例过低的文档块: {}", content.substring(0, Math.min(content.length(), 50)));
            return false;
        }
        
        // 通用的内容质量检查（不依赖特定领域）
        boolean hasSubstantiveContent = 
            // 包含基本的句子结构
            (content.contains("。") && content.contains("，")) ||
            // 包含冒号（通常表示定义或解释）
            content.contains("：") ||
            // 包含分号（表示列举或复杂句子）
            content.contains("；") ||
            // 包含常见的连接词（表示逻辑结构）
            content.contains("因为") || content.contains("所以") ||
            content.contains("如果") || content.contains("那么") ||
            content.contains("首先") || content.contains("其次") ||
            content.contains("然后") || content.contains("最后") ||
            // 包含常见的描述性词汇
            content.contains("是") || content.contains("为") ||
            content.contains("可以") || content.contains("能够") ||
            content.contains("用于") || content.contains("通过") ||
            content.contains("包括") || content.contains("具有") ||
            // 内容长度适中，可能包含有用信息
            (content.length() > 100 && content.length() < 2000) ||
            // 包含数字（可能是技术参数、步骤等）
            content.matches(".*\\d+.*");
        
        if (hasSubstantiveContent) {
            log.debug("保留有用的文档块: {}", content.substring(0, Math.min(content.length(), 100)));
            return true;
        }
        
        log.debug("过滤掉可能无用的文档块: {}", content.substring(0, Math.min(content.length(), 100)));
        return false;
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
            
            // 使用Spring AI VectorStore的delete方法，通过元数据过滤删除指定documentId的所有向量
            String filterExpression = String.format("documentId == '%s'", documentId);
            vectorStore.delete(filterExpression);
            
            log.info("成功从向量存储删除文档: {}", documentId);
            
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
            
            // 使用相似性搜索和过滤器来查找特定documentId的文档
            // 使用简单查询文本和元数据过滤器
            SearchRequest searchRequest = SearchRequest.builder()
                    .query("test") // 提供一个查询文本（某些实现可能需要）
                    .topK(1000) // 获取足够多的结果
                    .filterExpression(String.format("documentId == '%s'", documentId))
                    .similarityThreshold(0.0) // 最低相似度阈值
                    .build();
            
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            log.debug("找到 {} 个匹配的向量文档", results.size());
            
            return results;
            
        } catch (Exception e) {
            log.error("查找向量文档失败: {}", documentId, e);
            // 发生错误时返回空列表，不中断流程
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