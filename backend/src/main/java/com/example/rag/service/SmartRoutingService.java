package com.example.rag.service;

import com.example.rag.dto.SmartQueryResponse;
import com.example.rag.dto.StreamResponse;
import com.example.rag.model.Document;
import com.example.rag.model.DocumentChunk;
import com.example.rag.repository.DocumentRepository;
import com.example.rag.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Set;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 智能路由服务
 * 自动判断问题类型并选择最佳回答方式
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmartRoutingService {
    
    private final RagService ragService;
    private final VectorSearchService vectorSearchService;
    private final ChatClient chatClient;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    
    // 为了测试开发方便，暂时禁用缓存机制
    // private final Map<String, Boolean> queryConsistencyCache = new ConcurrentHashMap<>();
    // private static final int MAX_CACHE_SIZE = 1000;
    
    // 图书馆相关关键词（移除硬编码的技术词汇，保持通用性）
    private static final List<String> LIBRARY_KEYWORDS = Arrays.asList(
        "图书", "期刊", "论文", "数据库", "馆藏", "借阅", "文献", "资料", 
        "书籍", "杂志", "学术", "研究", "参考", "查阅", "检索", "索引"
        // 移除了硬编码的技术词汇，让系统更加通用
    );
    
    // 事实性查询模式
    private static final List<Pattern> FACTUAL_PATTERNS = Arrays.asList(
        Pattern.compile(".*什么是.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*如何.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*怎样.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*怎么.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*定义.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*解释.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*是什么.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*默认.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*端口.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*配置.*", Pattern.CASE_INSENSITIVE)
    );
    
    // 创意性问题模式
    private static final List<Pattern> CREATIVE_PATTERNS = Arrays.asList(
        Pattern.compile(".*写一个.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*创作.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*设计.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*想法.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*建议.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*帮我.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*生成.*", Pattern.CASE_INSENSITIVE)
    );
    
    // 向量相似度阈值常量
    private static final double SIMILARITY_THRESHOLD = 0.80; // 相似度阈值（实用标准）
    private static final double HIGH_SIMILARITY_THRESHOLD = 0.85; // 高相似度阈值（实用标准）
    
    // 性能监控类
    @lombok.Builder
    @lombok.Data
    private static class PerformanceMetrics {
        private long totalStartTime;
        private long vectorSearchStartTime;
        private long vectorSearchEndTime;
        private long contextBuildStartTime;
        private long contextBuildEndTime;
        private long aiProcessStartTime;
        private long aiProcessEndTime;
        private long totalEndTime;
        
        public long getVectorSearchDuration() {
            return vectorSearchEndTime - vectorSearchStartTime;
        }
        
        public long getContextBuildDuration() {
            return contextBuildEndTime - contextBuildStartTime;
        }
        
        public long getAiProcessDuration() {
            return aiProcessEndTime - aiProcessStartTime;
        }
        
        public long getTotalDuration() {
            return totalEndTime - totalStartTime;
        }
        
        public void logPerformanceBreakdown(String queryType) {
            log.info("🔍 {} 性能分解:", queryType);
            log.info("   📊 向量搜索: {}ms", getVectorSearchDuration());
            log.info("   🔨 上下文构建: {}ms", getContextBuildDuration());  
            log.info("   🤖 AI处理: {}ms", getAiProcessDuration());
            log.info("   ⏱️  总耗时: {}ms", getTotalDuration());
            log.info("   📈 向量搜索占比: {:.1f}%", (getVectorSearchDuration() * 100.0 / getTotalDuration()));
            log.info("   📈 上下文构建占比: {:.1f}%", (getContextBuildDuration() * 100.0 / getTotalDuration()));
            log.info("   📈 AI处理占比: {:.1f}%", (getAiProcessDuration() * 100.0 / getTotalDuration()));
        }
    }
    
    /**
     * 智能路由查询
     */
    public SmartQueryResponse smartQuery(String question) {
        PerformanceMetrics metrics = PerformanceMetrics.builder()
            .totalStartTime(System.currentTimeMillis())
            .build();
            
        // 添加空值检查
        if (question == null || question.trim().isEmpty()) {
            log.warn("收到空的查询请求");
            return SmartQueryResponse.builder()
                    .answer("请输入您的问题。")
                    .source("系统提示")
                    .sourceType(SmartQueryResponse.SourceType.SYSTEM)
                    .relevant(false)
                    .build();
        }
        
        question = question.trim();
        log.info("🚀 开始智能路由处理: {}", question);
        
        try {
            // 简单问候语直接回复
            if (isSimpleGreeting(question)) {
                metrics.setTotalEndTime(System.currentTimeMillis());
                log.info("⚡ 简单问候响应耗时: {}ms", metrics.getTotalDuration());
                return SmartQueryResponse.builder()
                        .answer("您好！我是RAG智能问答助手，可以帮您解答问题。有什么我可以帮助您的吗？")
                        .source("智能助手")
                        .sourceType(SmartQueryResponse.SourceType.GENERAL)
                        .relevant(true)
                        .build();
            }
            
            // 分析问题类型
            QuestionAnalysis analysis = analyzeQuestion(question);
            
            // 优先尝试图书馆资源（快速检索）
            SmartQueryResponse libraryResponse = tryLibraryResourcesFastWithMetrics(question, metrics);
            if (libraryResponse != null) {
                metrics.setTotalEndTime(System.currentTimeMillis());
                metrics.logPerformanceBreakdown("📚 基于文档查询");
                log.info("✅ 图书馆资源成功提供答案");
                return libraryResponse;
            }
            
            // 图书馆资源无法提供相关信息，切换到通用AI
            log.info("🤖 图书馆资源无法提供相关信息，切换到通用AI");
            SmartQueryResponse generalResponse = useGeneralAIWithMetrics(question, metrics);
            metrics.setTotalEndTime(System.currentTimeMillis());
            metrics.logPerformanceBreakdown("🤖 通用AI查询");
            return generalResponse;
            
        } catch (Exception e) {
            metrics.setTotalEndTime(System.currentTimeMillis());
            log.error("❌ 智能路由处理失败，耗时: {}ms", metrics.getTotalDuration(), e);
            return SmartQueryResponse.builder()
                    .answer("抱歉，处理您的问题时发生了错误，请稍后重试。")
                    .source("系统错误")
                    .sourceType(SmartQueryResponse.SourceType.SYSTEM)
                    .relevant(false)
                    .build();
        }
    }
    
    /**
     * 流式智能查询处理
     */
    public SseEmitter smartQueryStream(String question) {
        SseEmitter emitter = new SseEmitter(600000L); // 10分钟超时
        
        // 在新线程中处理，避免阻塞
        new Thread(() -> {
            try {
                log.info("开始流式智能路由处理: {}", question);
                
                // 简单问候语直接回复
                if (isSimpleGreeting(question)) {
                    emitter.send(StreamResponse.start("🤖 智能助手"));
                    emitter.send(StreamResponse.chunk("您好！我是RAG智能问答助手，可以帮您解答问题。有什么我可以帮助您的吗？"));
                    emitter.send(StreamResponse.end());
                    emitter.complete();
                    return;
                }
                
                // 预先进行质量检查，避免多次发送START响应
                boolean librarySuccess = tryLibraryResourcesStreamWithPreCheck(question, emitter);
                
                if (!librarySuccess) {
                    log.info("图书馆资源无法提供相关信息，使用通用AI");
                    // 直接发送通用AI的START响应并处理
                    emitter.send(StreamResponse.start("🤖 基于通用知识"));
                    useGeneralAIStreamWithoutStart(question, emitter);
                }
                
            } catch (Exception e) {
                log.error("流式智能路由处理失败", e);
                try {
                    emitter.send(StreamResponse.error("抱歉，处理您的问题时发生了错误，请稍后重试。"));
                    emitter.complete();
                } catch (IOException ioException) {
                    log.error("发送错误响应失败", ioException);
                    emitter.completeWithError(ioException);
                }
            }
        }).start();
        
        return emitter;
    }
    
    /**
     * 分析问题类型
     */
    private QuestionAnalysis analyzeQuestion(String question) {
        String lowerQuestion = question.toLowerCase();
        
        // 检查图书馆相关关键词
        boolean hasLibraryKeywords = LIBRARY_KEYWORDS.stream()
                .anyMatch(keyword -> lowerQuestion.contains(keyword.toLowerCase()));
        
        // 检查事实性查询模式
        boolean isFactual = FACTUAL_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(question).matches());
        
        // 检查创意性问题模式
        boolean isCreative = CREATIVE_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(question).matches());
        
        return QuestionAnalysis.builder()
                .preferLibraryResources(hasLibraryKeywords)
                .isFactual(isFactual)
                .isCreative(isCreative)
                .build();
    }
    
    /**
     * 判断是否为简单问候语
     */
    private boolean isSimpleGreeting(String question) {
        String lowerQuestion = question.toLowerCase().trim();
        List<String> greetings = Arrays.asList(
            "你好", "您好", "hello", "hi", "嗨", "早上好", "下午好", "晚上好",
            "谢谢", "感谢", "再见", "拜拜", "bye", "thanks", "thank you"
        );
        
        return greetings.stream().anyMatch(greeting -> 
            lowerQuestion.equals(greeting) || 
            lowerQuestion.startsWith(greeting + " ") ||
            lowerQuestion.startsWith(greeting + "，") ||
            lowerQuestion.startsWith(greeting + "。")
        );
    }
    
    /**
     * 尝试图书馆资源（带性能监控）- 基于客观相似度判断
     */
    private SmartQueryResponse tryLibraryResourcesFastWithMetrics(String question, PerformanceMetrics metrics) {
        try {
            // 开始向量搜索
            metrics.setVectorSearchStartTime(System.currentTimeMillis());
            log.info("🔍 开始向量搜索...");
            
            // 第一步：使用高阈值搜索，寻找高度相关的文档
            List<DocumentChunk> highRelevantChunks = vectorSearchService.vectorSearchWithThreshold(question, 3, HIGH_SIMILARITY_THRESHOLD);
            
            List<DocumentChunk> relevantChunks;
            if (!highRelevantChunks.isEmpty()) {
                log.info("✅ 找到 {} 个高度相关的文档块（阈值: {}）", highRelevantChunks.size(), HIGH_SIMILARITY_THRESHOLD);
                relevantChunks = highRelevantChunks;
            } else {
                // 第二步：如果没有高度相关文档，尝试使用标准阈值
                log.info("🔍 未找到高度相关文档，尝试标准阈值搜索...");
                List<DocumentChunk> standardRelevantChunks = vectorSearchService.vectorSearchWithThreshold(question, 3, SIMILARITY_THRESHOLD);
                
                if (!standardRelevantChunks.isEmpty()) {
                    log.info("✅ 找到 {} 个标准相关的文档块（阈值: {}）", standardRelevantChunks.size(), SIMILARITY_THRESHOLD);
                    relevantChunks = standardRelevantChunks;
                } else {
                    log.info("❌ 没有找到相似度足够的相关文档，判定为不相关");
                    metrics.setVectorSearchEndTime(System.currentTimeMillis());
                    log.info("🔍 向量搜索完成，耗时: {}ms，未找到相关文档", metrics.getVectorSearchDuration());
                    return null;
                }
            }
            
            metrics.setVectorSearchEndTime(System.currentTimeMillis());
            log.info("🔍 向量搜索完成，耗时: {}ms，找到 {} 个文档块", 
                metrics.getVectorSearchDuration(), relevantChunks.size());
            
            // 开始上下文构建
            metrics.setContextBuildStartTime(System.currentTimeMillis());
            log.info("🔨 开始构建上下文...");
            
            // 提取文档来源（这也是上下文构建的一部分）
            List<String> sources = relevantChunks.stream()
                    .map(chunk -> {
                        Optional<Document> document = documentRepository.findByDocumentId(chunk.getDocumentId());
                        return document.map(Document::getOriginalFilename).orElse(null);
                    })
                    .filter(filename -> filename != null)
                    .distinct()
                    .collect(Collectors.toList());
            
            metrics.setContextBuildEndTime(System.currentTimeMillis());
            log.info("🔨 上下文构建完成，耗时: {}ms，提取来源: {}", 
                metrics.getContextBuildDuration(), sources);
            
            // 开始AI处理
            metrics.setAiProcessStartTime(System.currentTimeMillis());
            log.info("🤖 开始AI处理...");
            
            // 使用单轮RAG查询（不使用多轮查询以提高速度）
            String ragAnswer = ragService.queryWithChunksSingleRound(question, relevantChunks);
            
            metrics.setAiProcessEndTime(System.currentTimeMillis());
            log.info("🤖 AI处理完成，耗时: {}ms，生成答案长度: {} 字符", 
                metrics.getAiProcessDuration(), ragAnswer != null ? ragAnswer.length() : 0);
            
            log.info("✅ 基于客观相似度判断的文档查询成功");
            
            return SmartQueryResponse.builder()
                    .answer(ragAnswer)
                    .source("📚 基于图书馆资源")
                    .sourceType(SmartQueryResponse.SourceType.LIBRARY)
                    .sources(sources)
                    .relevant(true)
                    .build();
                    
        } catch (Exception e) {
            if (metrics.getVectorSearchStartTime() > 0 && metrics.getVectorSearchEndTime() == 0) {
                metrics.setVectorSearchEndTime(System.currentTimeMillis());
                log.error("❌ 向量搜索阶段失败，耗时: {}ms", metrics.getVectorSearchDuration(), e);
            } else if (metrics.getContextBuildStartTime() > 0 && metrics.getContextBuildEndTime() == 0) {
                metrics.setContextBuildEndTime(System.currentTimeMillis());
                log.error("❌ 上下文构建阶段失败，耗时: {}ms", metrics.getContextBuildDuration(), e);
            } else if (metrics.getAiProcessStartTime() > 0 && metrics.getAiProcessEndTime() == 0) {
                metrics.setAiProcessEndTime(System.currentTimeMillis());
                log.error("❌ AI处理阶段失败，耗时: {}ms", metrics.getAiProcessDuration(), e);
            } else {
                log.error("❌ 快速图书馆资源查询失败", e);
            }
            return null;
        }
    }
    
    /**
     * 使用通用AI（带性能监控）
     */
    private SmartQueryResponse useGeneralAIWithMetrics(String question, PerformanceMetrics metrics) {
        try {
            // 通用AI没有向量搜索和上下文构建步骤，直接开始AI处理
            metrics.setVectorSearchStartTime(System.currentTimeMillis());
            metrics.setVectorSearchEndTime(System.currentTimeMillis()); // 立即结束，耗时为0
            
            metrics.setContextBuildStartTime(System.currentTimeMillis());
            metrics.setContextBuildEndTime(System.currentTimeMillis()); // 立即结束，耗时为0
            
            metrics.setAiProcessStartTime(System.currentTimeMillis());
            log.info("🤖 开始通用AI处理...");
            
            String prompt = String.format(
                "你是一个专业的AI助手。请详细回答以下问题，提供准确、全面、有用的信息。\n\n" +
                "问题：%s\n\n" +
                "回答要求：\n" +
                "1. 请用中文回答，保持回答的准确性和实用性\n" +
                "2. 提供完整、详细的信息，不要简略回答\n" +
                "3. 如果是复杂话题，请分层次、分要点详细阐述\n" +
                "4. 使用清晰的段落结构和适当的格式\n" +
                "5. 确保回答完整，不要在中途停止\n\n" +
                "请开始详细回答：", 
                question
            );
            
            String answer = chatClient.prompt(prompt).call().content();
            
            metrics.setAiProcessEndTime(System.currentTimeMillis());
            log.info("🤖 通用AI处理完成，耗时: {}ms，生成答案长度: {} 字符", 
                metrics.getAiProcessDuration(), answer != null ? answer.length() : 0);
            
            return SmartQueryResponse.builder()
                    .answer(answer)
                    .source("🧠 基于通用知识")
                    .sourceType(SmartQueryResponse.SourceType.GENERAL)
                    .note("此回答基于AI的通用知识，建议查阅相关专业资料进行验证")
                    .relevant(true)
                    .build();
                    
        } catch (Exception e) {
            if (metrics.getAiProcessStartTime() > 0 && metrics.getAiProcessEndTime() == 0) {
                metrics.setAiProcessEndTime(System.currentTimeMillis());
                log.error("❌ 通用AI处理失败，耗时: {}ms", metrics.getAiProcessDuration(), e);
            } else {
                log.error("❌ 通用AI查询失败", e);
            }
            return SmartQueryResponse.builder()
                    .answer("抱歉，无法处理您的问题，请稍后重试。")
                    .source("🧠 基于通用知识")
                    .sourceType(SmartQueryResponse.SourceType.GENERAL)
                    .relevant(false)
                    .build();
        }
    }
    
    /**
     * 使用通用AI进行流式查询（不发送START响应）
     */
    private void useGeneralAIStreamWithoutStart(String question, SseEmitter emitter) {
        try {
            log.info("使用通用AI进行流式查询（无START响应）: {}", question);
            
            String prompt = String.format(
                "你是一个专业的AI助手。请详细回答以下问题，提供准确、全面、有用的信息。\n\n" +
                "问题：%s\n\n" +
                "回答要求：\n" +
                "1. 请用中文回答，保持回答的准确性和实用性\n" +
                "2. 提供完整、详细的信息，不要简略回答\n" +
                "3. 如果是复杂话题，请分层次、分要点详细阐述\n" +
                "4. 使用清晰的段落结构和适当的格式\n" +
                "5. 确保回答完整，不要在中途停止\n\n" +
                "请开始详细回答：", 
                question
            );
            
            // 使用原子布尔值跟踪emitter状态
            final AtomicBoolean emitterCompleted = new AtomicBoolean(false);
            
            // 使用流式调用
            chatClient.prompt(prompt).stream().content()
                .doOnNext(chunk -> {
                    if (!emitterCompleted.get()) {
                        try {
                            // 直接发送原始内容，让前端处理思考过程分离
                            if (!chunk.trim().isEmpty()) {
                                emitter.send(StreamResponse.chunk(chunk));
                            }
                        } catch (IOException e) {
                            log.error("发送流式内容失败", e);
                            if (!emitterCompleted.compareAndSet(false, true)) {
                                return; // 已经完成，直接返回
                            }
                            try {
                                emitter.completeWithError(e);
                            } catch (Exception ex) {
                                log.error("完成emitter失败", ex);
                            }
                        }
                    }
                })
                .doOnComplete(() -> {
                    if (!emitterCompleted.compareAndSet(false, true)) {
                        return; // 已经完成，直接返回
                    }
                    try {
                        log.info("通用AI流式响应完成");
                        emitter.send(StreamResponse.note("此回答基于AI的通用知识，建议查阅相关专业资料进行验证"));
                        emitter.send(StreamResponse.end());
                        emitter.complete();
                    } catch (IOException e) {
                        log.error("完成流式响应失败", e);
                        try {
                            emitter.completeWithError(e);
                        } catch (Exception ex) {
                            log.error("完成emitter失败", ex);
                        }
                    }
                })
                .doOnError(error -> {
                    if (!emitterCompleted.compareAndSet(false, true)) {
                        return; // 已经完成，直接返回
                    }
                    log.error("通用AI流式查询失败", error);
                    try {
                        emitter.send(StreamResponse.error("抱歉，无法处理您的问题，请稍后重试。"));
                        emitter.complete();
                    } catch (IOException e) {
                        log.error("发送错误响应失败", e);
                        try {
                            emitter.completeWithError(e);
                        } catch (Exception ex) {
                            log.error("完成emitter失败", ex);
                        }
                    }
                })
                .subscribe();
                
        } catch (Exception e) {
            log.error("通用AI流式查询失败", e);
            try {
                emitter.send(StreamResponse.error("抱歉，无法处理您的问题，请稍后重试。"));
                emitter.complete();
            } catch (IOException ioException) {
                log.error("发送错误响应失败", ioException);
                try {
                    emitter.completeWithError(ioException);
                } catch (Exception ex) {
                    log.error("完成emitter失败", ex);
                }
            }
        }
    }
    
    /**
     * 使用通用AI进行流式查询（发送START响应）
     */
    private void useGeneralAIStream(String question, SseEmitter emitter) {
        try {
            log.info("使用通用AI进行流式查询: {}", question);
            
            // 发送开始响应
            emitter.send(StreamResponse.start("🤖 基于通用知识"));
            
            // 调用不发送START响应的版本
            useGeneralAIStreamWithoutStart(question, emitter);
                
        } catch (Exception e) {
            log.error("通用AI流式查询失败", e);
            try {
                emitter.send(StreamResponse.error("抱歉，无法处理您的问题，请稍后重试。"));
                emitter.complete();
            } catch (IOException ioException) {
                log.error("发送错误响应失败", ioException);
                try {
                    emitter.completeWithError(ioException);
                } catch (Exception ex) {
                    log.error("完成emitter失败", ex);
                }
            }
        }
    }
    
    /**
     * 带预检查的快速流式图书馆资源查询 - 基于客观相似度判断
     */
    private boolean tryLibraryResourcesStreamWithPreCheck(String question, SseEmitter emitter) {
        try {
            // 第一步：使用高阈值搜索，寻找高度相关的文档
            List<DocumentChunk> highRelevantChunks = vectorSearchService.vectorSearchWithThreshold(question, 3, HIGH_SIMILARITY_THRESHOLD);
            
            List<DocumentChunk> relevantChunks;
            if (!highRelevantChunks.isEmpty()) {
                log.info("✅ 流式查询找到 {} 个高度相关的文档块（阈值: {}）", highRelevantChunks.size(), HIGH_SIMILARITY_THRESHOLD);
                relevantChunks = highRelevantChunks;
            } else {
                // 第二步：如果没有高度相关文档，尝试使用标准阈值
                log.info("🔍 流式查询未找到高度相关文档，尝试标准阈值搜索...");
                List<DocumentChunk> standardRelevantChunks = vectorSearchService.vectorSearchWithThreshold(question, 3, SIMILARITY_THRESHOLD);
                
                if (!standardRelevantChunks.isEmpty()) {
                    log.info("✅ 流式查询找到 {} 个标准相关的文档块（阈值: {}）", standardRelevantChunks.size(), SIMILARITY_THRESHOLD);
                    relevantChunks = standardRelevantChunks;
                } else {
                    log.info("❌ 流式查询没有找到相似度足够的相关文档，判定为不相关");
                    return false; // 返回false表示无法提供相关信息
                }
            }
            
            log.info("✅ 基于客观相似度判断，文档内容相关，开始基于文档的流式输出");
            
            // 发送基于文档的START响应
            emitter.send(StreamResponse.start("📚 基于图书馆资源"));
            
            // 提取文档来源
            List<String> sources = relevantChunks.stream()
                    .map(chunk -> {
                        log.debug("查找文档ID: {}", chunk.getDocumentId());
                        Optional<Document> document = documentRepository.findByDocumentId(chunk.getDocumentId());
                        if (document.isPresent()) {
                            log.debug("找到文档: {}", document.get().getOriginalFilename());
                            return document.get().getOriginalFilename();
                        } else {
                            log.warn("未找到文档ID: {} 对应的文档记录，跳过此来源", chunk.getDocumentId());
                            return null; // 返回null，后续会被过滤掉
                        }
                    })
                    .filter(filename -> filename != null) // 过滤掉null值
                    .distinct()
                    .collect(Collectors.toList());
            
            log.info("提取的文档来源: {}", sources);
            
            // 直接构建上下文并进行流式输出
            String context = buildFastContext(relevantChunks);
            generateFastStreamResponse(question, context, emitter, sources);
            
            return true;
                    
        } catch (Exception e) {
            log.error("预检查图书馆资源流式查询失败", e);
            return false; // 返回false表示查询失败
        }
    }
    
    /**
     * 缓存查询结果以确保一致性
     * 为了测试开发方便，暂时禁用此方法
     */
    /*
    private void cacheQueryResult(String cacheKey, boolean useDocument) {
        // 简单的LRU策略：如果缓存太大，清理一半
        if (queryConsistencyCache.size() >= MAX_CACHE_SIZE) {
            log.info("查询缓存已满，清理旧条目");
            queryConsistencyCache.clear(); // 简单清理策略
        }
        
        queryConsistencyCache.put(cacheKey, useDocument);
        log.debug("缓存查询结果: {} -> {}", cacheKey, useDocument ? "文档" : "通用AI");
    }
    */
    
    /**
     * 快速构建上下文
     */
    private String buildFastContext(List<DocumentChunk> chunks) {
        StringBuilder contextBuilder = new StringBuilder();
        int currentLength = 0;
        int fastMaxLength = 2000; // 进一步减少上下文长度以提高速度
        
        log.info("快速构建上下文，最大长度: {} 字符", fastMaxLength);
        
        for (DocumentChunk chunk : chunks) {
            String chunkContent = chunk.getContent();
            
            // 检查是否超过最大上下文长度
            if (currentLength + chunkContent.length() > fastMaxLength) {
                // 截取部分内容
                int remainingLength = fastMaxLength - currentLength;
                if (remainingLength > 100) { // 至少保留100字符
                    chunkContent = chunkContent.substring(0, remainingLength) + "...";
                    contextBuilder.append(chunkContent).append("\n\n");
                }
                break;
            }
            
            contextBuilder.append(chunkContent).append("\n\n");
            currentLength += chunkContent.length() + 2; // +2 for \n\n
        }
        
        String context = contextBuilder.toString().trim();
        log.info("快速构建的上下文长度: {} 字符", context.length());
        
        return context;
    }
    
    /**
     * 生成快速流式响应
     */
    private void generateFastStreamResponse(String question, String context, SseEmitter emitter, List<String> sources) {
        try {
            // 使用与RAG_PROMPT_TEMPLATE一致的提示模板，包含思考过程
            String fastPrompt = String.format(
                "你是一个专业的AI助手。请基于以下提供的文档内容来回答用户的问题。\n\n" +
                "文档内容：\n%s\n\n" +
                "用户问题：%s\n\n" +
                "请遵循以下规则：\n" +
                "1. 仔细分析文档内容，包括直接陈述和间接表述\n" +
                "2. 基于文档内容进行合理的推理和理解\n" +
                "3. 如果文档中没有直接的相关信息，请直接基于你的通用知识给出准确的答案\n" +
                "4. 不要提及\"文档中没有找到\"或类似的表述，直接给出有用的答案\n" +
                "5. 回答要准确、简洁、有条理\n" +
                "6. 如果可能，请引用具体的文档内容\n" +
                "7. 使用中文回答\n" +
                "8. **重要**：必须先在<think>标签中展示你的思考过程，然后在</think>标签后给出正式答案\n" +
                "9. **格式要求**：\n" +
                "   - 使用清晰的段落结构，每个要点之间用空行分隔\n" +
                "   - 使用序号（1. 2. 3.）或项目符号（- ）来组织列表\n" +
                "   - 重要概念用**粗体**标记\n" +
                "   - 代码或技术术语用`反引号`标记\n" +
                "   - 使用恰当的标点符号和换行\n" +
                "   - 保持逻辑清晰，结构完整\n\n" +
                "回答格式（必须遵循）：\n" +
                "<think>\n" +
                "这里写出你的分析思考过程，包括对文档内容的理解、问题的分析、推理过程等。\n" +
                "</think>\n\n" +
                "**正式回答：**\n\n" +
                "[在这里给出格式良好、结构清晰的正式答案，遵循上述格式要求]\n\n" +
                "回答：",
                context, question
            );
            
            log.info("发送快速流式提示到AI模型");
            
            // 使用流式调用
            chatClient.prompt(fastPrompt).stream().content()
                .doOnNext(chunk -> {
                    try {
                        // 添加详细的chunk日志
                        log.info("🔍 接收到流式chunk: [{}]", chunk);
                        
                        // 直接发送内容，不过滤思考标签（因为我们已经要求不要思考过程）
                        if (!chunk.trim().isEmpty()) {
                            log.info("📤 发送流式chunk: [{}]", chunk);
                            emitter.send(StreamResponse.chunk(chunk));
                        } else {
                            log.info("🚫 跳过空chunk");
                        }
                    } catch (IOException e) {
                        log.error("发送快速流式内容失败", e);
                    }
                })
                .doOnComplete(() -> {
                    try {
                        log.info("快速流式AI回答生成完成");
                        // 发送来源信息和结束事件
                        if (sources != null) {
                            emitter.send(StreamResponse.source(sources));
                        }
                        emitter.send(StreamResponse.end());
                        emitter.complete();
                    } catch (IOException e) {
                        log.error("完成快速流式响应失败", e);
                        emitter.completeWithError(e);
                    }
                })
                .doOnError(error -> {
                    log.error("生成快速流式AI回答失败", error);
                    try {
                        emitter.send(StreamResponse.error("抱歉，生成回答时发生了错误。"));
                        if (sources != null) {
                            emitter.send(StreamResponse.source(sources));
                        }
                        emitter.send(StreamResponse.end());
                        emitter.complete();
                    } catch (IOException e) {
                        log.error("发送错误响应失败", e);
                        emitter.completeWithError(e);
                    }
                })
                .subscribe();
                
        } catch (Exception e) {
            log.error("生成快速流式AI回答失败", e);
            try {
                emitter.send(StreamResponse.error("抱歉，生成回答时发生了错误。"));
                if (sources != null) {
                    emitter.send(StreamResponse.source(sources));
                }
                emitter.send(StreamResponse.end());
                emitter.complete();
            } catch (IOException ioException) {
                log.error("发送错误响应失败", ioException);
                emitter.completeWithError(ioException);
            }
        }
    }
    
    /**
     * 提取实际的回答内容（去除思考标签）
     */
    private String extractActualAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        
        log.info("🔍 原始AI回答长度: {} 字符", answer.length());
        log.info("🔍 原始AI回答前200字符: {}", answer.substring(0, Math.min(200, answer.length())));
        
        // 如果包含思考标签，提取思考标签之外的内容
        if (answer.contains("<think>")) {
            log.info("🔍 检测到思考标签，开始提取实际答案");
            
            // 查找</think>标签
            if (answer.contains("</think>")) {
                // 找到最后一个</think>标签的位置
                int lastThinkEndIndex = answer.lastIndexOf("</think>");
                if (lastThinkEndIndex != -1) {
                    String afterThink = answer.substring(lastThinkEndIndex + 8).trim();
                    log.info("🔍 </think>标签后的内容长度: {} 字符", afterThink.length());
                    if (afterThink.length() > 0) {
                        log.info("🔍 </think>标签后的内容前100字符: {}", afterThink.substring(0, Math.min(100, afterThink.length())));
                        log.info("✅ 使用</think>后的内容作为实际答案");
                        return afterThink;
                    }
                }
            } else {
                log.warn("⚠️ 检测到<think>但没有找到</think>，AI回答格式不规范");
            }
            
            // 如果</think>后面没有内容，提取<think>之前的内容
            int firstThinkStartIndex = answer.indexOf("<think>");
            if (firstThinkStartIndex > 0) {
                String beforeThink = answer.substring(0, firstThinkStartIndex).trim();
                log.info("🔍 <think>标签前的内容长度: {} 字符", beforeThink.length());
                if (beforeThink.length() > 0) {
                    log.info("🔍 <think>标签前的内容前100字符: {}", beforeThink.substring(0, Math.min(100, beforeThink.length())));
                    log.info("✅ 使用<think>前的内容作为实际答案");
                    return beforeThink;
                }
            }
            
            // 如果前后都没有内容，说明整个回答都是思考过程，这种情况下应该返回空或者使用通用AI
            log.warn("⚠️ 思考标签前后都没有实际内容，AI回答可能完全是思考过程");
            
            // 最后的兜底策略：如果整个回答都被思考标签包围，尝试移除思考标签
            if (answer.contains("</think>")) {
                // 尝试完全移除思考标签区域
                String result = answer.replaceAll("<think>.*?</think>", "").trim();
                if (result.length() > 20) {
                    log.info("🔧 移除思考标签后剩余内容长度: {} 字符", result.length());
                    log.info("🔧 移除思考标签后的内容前100字符: {}", result.substring(0, Math.min(100, result.length())));
                    return result;
                }
            }
            
            // 如果所有方法都失败，返回空字符串，让系统使用通用AI
            log.warn("❌ 无法从思考标签中提取有效的实际答案，返回空字符串");
            return "";
        }
        
        // 如果没有思考标签，返回原始内容
        log.info("✅ 没有思考标签，使用原始内容作为实际答案");
        return answer.trim();
    }
    
    /**
     * 问题分析结果
     */
    @lombok.Builder
    @lombok.Data
    private static class QuestionAnalysis {
        private boolean preferLibraryResources;
        private boolean isFactual;
        private boolean isCreative;
    }
} 