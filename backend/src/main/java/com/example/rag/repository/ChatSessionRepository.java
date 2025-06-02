package com.example.rag.repository;

import com.example.rag.model.ChatSession;
import com.example.rag.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 聊天会话仓储接口
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    
    /**
     * 根据会话ID查找会话
     */
    Optional<ChatSession> findBySessionId(String sessionId);
    
    /**
     * 根据用户查找会话
     */
    Page<ChatSession> findByUser(User user, Pageable pageable);
    
    /**
     * 根据用户查找活跃会话
     */
    Page<ChatSession> findByUserAndIsActive(User user, Boolean isActive, Pageable pageable);
    
    /**
     * 根据用户查找最近的会话
     */
    @Query("SELECT cs FROM ChatSession cs WHERE cs.user = :user ORDER BY cs.updatedAt DESC")
    List<ChatSession> findRecentSessionsByUser(@Param("user") User user, Pageable pageable);
    
    /**
     * 查找用户的活跃会话数量
     */
    @Query("SELECT COUNT(cs) FROM ChatSession cs WHERE cs.user = :user AND cs.isActive = true")
    long countActiveSessionsByUser(@Param("user") User user);
    
    /**
     * 查找指定时间范围内的会话
     */
    @Query("SELECT cs FROM ChatSession cs WHERE cs.createdAt BETWEEN :startTime AND :endTime")
    List<ChatSession> findSessionsBetween(@Param("startTime") LocalDateTime startTime, 
                                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 根据标题模糊查找会话
     */
    @Query("SELECT cs FROM ChatSession cs WHERE cs.user = :user AND cs.title LIKE %:title%")
    Page<ChatSession> findByUserAndTitleContaining(@Param("user") User user, 
                                                   @Param("title") String title, 
                                                   Pageable pageable);
    
    /**
     * 查找长时间未更新的会话
     */
    @Query("SELECT cs FROM ChatSession cs WHERE cs.updatedAt < :cutoffTime AND cs.isActive = true")
    List<ChatSession> findStaleActiveSessions(@Param("cutoffTime") LocalDateTime cutoffTime);
} 