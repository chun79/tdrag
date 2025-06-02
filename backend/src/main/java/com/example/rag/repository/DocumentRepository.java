package com.example.rag.repository;

import com.example.rag.model.Document;
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
 * 文档仓储接口
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    
    /**
     * 根据文档ID查找文档
     */
    Optional<Document> findByDocumentId(String documentId);
    
    /**
     * 根据用户查找文档
     */
    Page<Document> findByUploadUser(User user, Pageable pageable);
    
    /**
     * 根据状态查找文档
     */
    List<Document> findByStatus(Document.DocumentStatus status);
    
    /**
     * 根据分类查找文档
     */
    Page<Document> findByCategory(String category, Pageable pageable);
    
    /**
     * 根据用户和状态查找文档
     */
    Page<Document> findByUploadUserAndStatus(User user, Document.DocumentStatus status, Pageable pageable);
    
    /**
     * 根据文件名模糊查找
     */
    @Query("SELECT d FROM Document d WHERE d.originalFilename LIKE %:filename%")
    Page<Document> findByFilenameContaining(@Param("filename") String filename, Pageable pageable);
    
    /**
     * 查找指定时间范围内的文档
     */
    @Query("SELECT d FROM Document d WHERE d.uploadTime BETWEEN :startTime AND :endTime")
    List<Document> findByUploadTimeBetween(@Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计用户文档数量
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadUser = :user AND d.status != 'DELETED'")
    long countByUploadUserAndStatusNot(@Param("user") User user);
    
    /**
     * 查找处理失败的文档
     */
    @Query("SELECT d FROM Document d WHERE d.status = 'FAILED' AND d.uploadTime > :since")
    List<Document> findFailedDocumentsSince(@Param("since") LocalDateTime since);
} 