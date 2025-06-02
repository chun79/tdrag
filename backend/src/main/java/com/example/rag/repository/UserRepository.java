package com.example.rag.repository;

import com.example.rag.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 用户仓储接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 查找活跃用户
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true")
    java.util.List<User> findActiveUsers();
    
    /**
     * 根据角色查找用户
     */
    java.util.List<User> findByRole(User.UserRole role);
    
    /**
     * 根据用户名或邮箱查找用户
     */
    @Query("SELECT u FROM User u WHERE u.username = :identifier OR u.email = :identifier")
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);
} 