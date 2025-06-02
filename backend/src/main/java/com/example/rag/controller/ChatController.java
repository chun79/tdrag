package com.example.rag.controller;

import com.example.rag.dto.ChatRequest;
import com.example.rag.dto.ChatResponse;
import com.example.rag.dto.QueryRequest;
import com.example.rag.dto.QueryResponse;
import com.example.rag.dto.SmartQueryResponse;
import com.example.rag.dto.StreamResponse;
import com.example.rag.model.ChatMessage;
import com.example.rag.model.ChatSession;
import com.example.rag.model.User;
import com.example.rag.service.ChatService;
import com.example.rag.service.SmartRoutingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 聊天控制器
 * 处理RAG问答相关的API请求
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "聊天问答API")
@CrossOrigin(origins = "*")
public class ChatController {
    
    private final ChatService chatService;
    private final SmartRoutingService smartRoutingService;
    
    /**
     * 发送聊天消息
     */
    @PostMapping("/send")
    public ResponseEntity<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        try {
            // TODO: 从认证上下文获取用户
            User user = getCurrentUser();
            
            ChatResponse response = chatService.chat(request, user);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return ResponseEntity.badRequest()
                    .body(ChatResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .build());
        }
    }
    
    /**
     * 智能查询聊天回复（新的智能路由API）
     */
    @PostMapping("/query")
    public ResponseEntity<QueryResponse> queryMessage(@RequestBody QueryRequest request) {
        try {
            log.info("收到智能查询请求: {}", request.getActualQuestion());
            
            // 使用智能路由服务
            SmartQueryResponse smartResponse = smartRoutingService.smartQuery(request.getActualQuestion());
            
            // 转换为前端期望的格式
            QueryResponse queryResponse = QueryResponse.builder()
                    .answer(smartResponse.getAnswer())
                    .sources(smartResponse.getSources())
                    .sourceType(smartResponse.getSource())
                    .note(smartResponse.getNote())
                    .build();
            
            log.info("智能路由返回响应，来源: {}", smartResponse.getSource());
            return ResponseEntity.ok(queryResponse);
            
        } catch (Exception e) {
            log.error("智能查询失败", e);
            return ResponseEntity.ok(QueryResponse.builder()
                    .answer("抱歉，发生了错误，请稍后重试。")
                    .sourceType("系统错误")
                    .build());
        }
    }
    
    /**
     * 流式智能查询聊天回复
     */
    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter queryMessageStream(@RequestBody QueryRequest request) {
        try {
            log.info("收到流式智能查询请求: {}", request.getActualQuestion());
            
            // 使用智能路由服务的流式方法
            return smartRoutingService.smartQueryStream(request.getActualQuestion());
            
        } catch (Exception e) {
            log.error("流式智能查询失败", e);
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(StreamResponse.error("抱歉，发生了错误，请稍后重试。"));
                emitter.complete();
            } catch (Exception sendError) {
                log.error("发送错误响应失败", sendError);
                emitter.completeWithError(sendError);
            }
            return emitter;
        }
    }
    
    /**
     * 获取聊天历史
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable String sessionId) {
        try {
            User user = getCurrentUser();
            List<ChatMessage> history = chatService.getChatHistory(sessionId, user);
            return ResponseEntity.ok(history);
            
        } catch (Exception e) {
            log.error("获取聊天历史失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取用户会话列表
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSession>> getUserSessions() {
        try {
            User user = getCurrentUser();
            List<ChatSession> sessions = chatService.getUserSessions(user);
            return ResponseEntity.ok(sessions);
            
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 创建新会话
     */
    @PostMapping("/sessions")
    public ResponseEntity<ChatSession> createSession(@RequestParam(required = false) String title) {
        try {
            User user = getCurrentUser();
            ChatSession session = chatService.createSession(user, title);
            return ResponseEntity.ok(session);
            
        } catch (Exception e) {
            log.error("创建会话失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        try {
            User user = getCurrentUser();
            chatService.deleteSession(sessionId, user);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("删除会话失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取当前用户（临时实现）
     */
    private User getCurrentUser() {
        // TODO: 从Spring Security上下文获取当前用户
        // 暂时返回模拟用户，先检查是否存在，不存在则创建
        String username = "testuser";
        String email = "test@example.com";
        
        // 这里应该注入UserService，但为了简化，直接返回固定用户
        return User.builder()
                .id(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .username(username)
                .email(email)
                .role(User.UserRole.USER)
                .build();
    }
} 