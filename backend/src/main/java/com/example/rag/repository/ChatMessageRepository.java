package com.example.rag.repository;

import com.example.rag.model.ChatMessage;
import com.example.rag.model.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 聊天消息仓储接口
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    
    /**
     * 根据会话查找消息
     */
    Page<ChatMessage> findBySession(ChatSession session, Pageable pageable);
    
    /**
     * 根据会话查找消息（按时间排序）
     */
    List<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session);
    
    /**
     * 根据会话和角色查找消息
     */
    List<ChatMessage> findBySessionAndRole(ChatSession session, ChatMessage.MessageRole role);
    
    /**
     * 统计会话消息数量
     */
    long countBySession(ChatSession session);
    
    /**
     * 查找会话的最新消息
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.session = :session ORDER BY cm.createdAt DESC")
    List<ChatMessage> findLatestMessagesBySession(@Param("session") ChatSession session, Pageable pageable);
    
    /**
     * 查找指定时间范围内的消息
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.session = :session AND cm.createdAt BETWEEN :startTime AND :endTime")
    List<ChatMessage> findMessagesBetween(@Param("session") ChatSession session,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计用户总消息数
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.session.user.id = :userId")
    long countMessagesByUserId(@Param("userId") UUID userId);
    
    /**
     * 查找使用特定模型的消息
     */
    List<ChatMessage> findByModelUsed(String modelUsed);
    
    /**
     * 统计总token使用量
     */
    @Query("SELECT SUM(cm.tokensUsed) FROM ChatMessage cm WHERE cm.session.user.id = :userId AND cm.tokensUsed IS NOT NULL")
    Long sumTokensUsedByUserId(@Param("userId") UUID userId);
    
    /**
     * 查找响应时间超过阈值的消息
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.responseTimeMs > :threshold AND cm.responseTimeMs IS NOT NULL")
    List<ChatMessage> findSlowResponses(@Param("threshold") Long threshold);
} 