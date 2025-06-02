package com.example.rag.service;

import com.example.rag.dto.ChatRequest;
import com.example.rag.dto.ChatResponse;
import com.example.rag.model.ChatMessage;
import com.example.rag.model.ChatSession;
import com.example.rag.model.User;
import com.example.rag.repository.ChatMessageRepository;
import com.example.rag.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 聊天服务类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RagService ragService;
    private final ChatClient chatClient;
    
    @Value("${app.model.chat.fast:qwen3:14b}")
    private String fastModel;
    
    @Value("${app.model.chat.quality:qwen3:32b}")
    private String qualityModel;
    
    /**
     * 处理聊天请求
     */
    @Transactional
    public ChatResponse chat(ChatRequest request, User user) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取或创建会话
            ChatSession session = getOrCreateSession(request.getSessionId(), user);
            
            // 保存用户消息
            ChatMessage userMessage = saveUserMessage(session, request.getMessage());
            
            // 生成回复
            String response;
            List<String> sources = null;
            
            if (request.getUseRag()) {
                // 尝试使用RAG生成回复
                String ragResponse = ragService.query(request.getMessage());
                
                // 如果RAG找到了相关文档并生成了回复，使用RAG回复
                if (ragResponse != null && !ragResponse.trim().isEmpty() && 
                    !ragResponse.contains("没有找到与您问题相关的信息")) {
                    response = ragResponse;
                    sources = List.of("文档知识库");
                } else {
                    // 如果没有找到相关文档，使用基本AI对话
                    response = generateDirectResponse(request);
                }
            } else {
                // 直接使用LLM生成回复
                response = generateDirectResponse(request);
            }
            
            // 保存AI回复
            ChatMessage aiMessage = saveAiMessage(session, response, request.getModel());
            
            // 更新会话
            updateSession(session);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            return ChatResponse.builder()
                    .sessionId(session.getSessionId())
                    .messageId(aiMessage.getId().toString())
                    .response(response)
                    .sources(sources)
                    .modelUsed(request.getModel() != null ? request.getModel() : fastModel)
                    .responseTimeMs(responseTime)
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("聊天处理失败", e);
            long responseTime = System.currentTimeMillis() - startTime;
            
            return ChatResponse.builder()
                    .sessionId(request.getSessionId())
                    .success(false)
                    .error(e.getMessage())
                    .responseTimeMs(responseTime)
                    .build();
        }
    }
    
    /**
     * 获取聊天历史
     */
    public List<ChatMessage> getChatHistory(String sessionId, User user) {
        Optional<ChatSession> sessionOpt = chatSessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            ChatSession session = sessionOpt.get();
            // 检查权限
            if (!session.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("无权限访问此会话");
            }
            return chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);
        }
        return List.of();
    }
    
    /**
     * 获取用户会话列表
     */
    public List<ChatSession> getUserSessions(User user) {
        return chatSessionRepository.findRecentSessionsByUser(user, 
                org.springframework.data.domain.PageRequest.of(0, 50));
    }
    
    /**
     * 创建新会话
     */
    @Transactional
    public ChatSession createSession(User user, String title) {
        String sessionId = UUID.randomUUID().toString();
        
        ChatSession session = ChatSession.builder()
                .sessionId(sessionId)
                .title(title != null ? title : "新对话")
                .user(user)
                .build();
                
        return chatSessionRepository.save(session);
    }
    
    /**
     * 删除会话
     */
    @Transactional
    public void deleteSession(String sessionId, User user) {
        Optional<ChatSession> sessionOpt = chatSessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            ChatSession session = sessionOpt.get();
            
            // 检查权限
            if (!session.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("无权限删除此会话");
            }
            
            // 标记为非活跃
            session.setIsActive(false);
            chatSessionRepository.save(session);
        }
    }
    
    /**
     * 获取或创建会话
     */
    private ChatSession getOrCreateSession(String sessionId, User user) {
        if (sessionId != null) {
            Optional<ChatSession> sessionOpt = chatSessionRepository.findBySessionId(sessionId);
            if (sessionOpt.isPresent()) {
                ChatSession session = sessionOpt.get();
                // 检查权限
                if (!session.getUser().getId().equals(user.getId())) {
                    throw new RuntimeException("无权限访问此会话");
                }
                return session;
            }
        }
        
        // 创建新会话
        return createSession(user, "新对话");
    }
    
    /**
     * 保存用户消息
     */
    private ChatMessage saveUserMessage(ChatSession session, String content) {
        log.info("保存用户消息，content: {}", content);
        
        if (content == null || content.trim().isEmpty()) {
            log.error("用户消息内容为空");
            throw new RuntimeException("消息内容不能为空");
        }
        
        ChatMessage message = ChatMessage.builder()
                .session(session)
                .role(ChatMessage.MessageRole.USER)
                .content(content)
                .build();
        
        log.info("创建的ChatMessage: {}", message);
        return chatMessageRepository.save(message);
    }
    
    /**
     * 保存AI消息
     */
    private ChatMessage saveAiMessage(ChatSession session, String content, String model) {
        ChatMessage message = ChatMessage.builder()
                .session(session)
                .role(ChatMessage.MessageRole.ASSISTANT)
                .content(content)
                .modelUsed(model != null ? model : fastModel)
                .build();
                
        return chatMessageRepository.save(message);
    }
    
    /**
     * 更新会话
     */
    private void updateSession(ChatSession session) {
        session.setUpdatedAt(LocalDateTime.now());
        session.setMessageCount(session.getMessageCount() + 2); // 用户消息 + AI回复
        chatSessionRepository.save(session);
    }
    
    /**
     * 直接生成回复（不使用RAG）
     */
    private String generateDirectResponse(ChatRequest request) {
        try {
            log.info("使用基本AI对话模式回答问题: {}", request.getMessage());
            
            // 使用Spring AI ChatClient调用Ollama
            String response = chatClient.prompt()
                    .user(request.getMessage())
                    .call()
                    .content();
            
            log.info("AI回复: {}", response);
            return response;
            
        } catch (Exception e) {
            log.error("生成回复失败", e);
            return "抱歉，我现在无法回答您的问题，请稍后再试。错误信息: " + e.getMessage();
        }
    }
} 