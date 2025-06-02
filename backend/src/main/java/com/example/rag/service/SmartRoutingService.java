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
    
    // 图书馆相关关键词
    private static final List<String> LIBRARY_KEYWORDS = Arrays.asList(
        "图书", "期刊", "论文", "数据库", "馆藏", "借阅", "文献", "资料", 
        "书籍", "杂志", "学术", "研究", "参考", "查阅", "检索", "索引",
        "mysql", "数据库", "sql", "编程", "技术", "配置", "安装", "设置"
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
    
    /**
     * 智能查询处理
     */
    public SmartQueryResponse smartQuery(String question) {
        try {
            log.info("开始智能路由处理: {}", question);
            
            // 1. 分析问题类型
            QuestionAnalysis analysis = analyzeQuestion(question);
            log.info("问题分析结果: {}", analysis);
            
            // 2. 对于简单问候语或非技术问题，直接使用通用AI
            if (isSimpleGreeting(question) || (!analysis.preferLibraryResources && !analysis.isFactual)) {
                log.info("简单问候语或非技术问题，直接使用通用AI");
                return useGeneralAI(question);
            }
            
            // 3. 优先尝试图书馆资源
            if (analysis.preferLibraryResources || analysis.isFactual) {
                SmartQueryResponse libraryResult = tryLibraryResources(question);
                
                // 如果找到相关内容，直接返回
                if (libraryResult != null && libraryResult.isRelevant()) {
                    log.info("使用图书馆资源回答");
                    return libraryResult;
                }
                
                log.info("图书馆资源无法提供相关信息，切换到通用AI");
                return useGeneralAI(question);
            }
            
            // 4. 创意性问题直接使用通用AI
            if (analysis.isCreative) {
                log.info("创意性问题，直接使用通用AI");
                return useGeneralAI(question);
            }
            
            // 5. 默认情况：尝试图书馆资源，失败则使用通用AI
            SmartQueryResponse libraryResult = tryLibraryResources(question);
            if (libraryResult != null && libraryResult.isRelevant()) {
                log.info("使用图书馆资源回答");
                return libraryResult;
            } else {
                log.info("图书馆资源无法提供相关信息，切换到通用AI");
                return useGeneralAI(question);
            }
            
        } catch (Exception e) {
            log.error("智能路由处理失败", e);
            return SmartQueryResponse.builder()
                    .answer("抱歉，处理您的问题时发生了错误，请稍后重试。")
                    .source("系统错误")
                    .sourceType(SmartQueryResponse.SourceType.ERROR)
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
                
                // 1. 分析问题类型
                QuestionAnalysis analysis = analyzeQuestion(question);
                log.info("问题分析结果: {}", analysis);
                
                // 2. 对于简单问候语或非技术问题，直接使用通用AI
                if (isSimpleGreeting(question) || (!analysis.preferLibraryResources && !analysis.isFactual)) {
                    log.info("简单问候语或非技术问题，直接使用通用AI");
                    useGeneralAIStream(question, emitter);
                    return;
                }
                
                // 3. 优先尝试图书馆资源
                if (analysis.preferLibraryResources || analysis.isFactual) {
                    boolean librarySuccess = tryLibraryResourcesStream(question, emitter);
                    
                    if (!librarySuccess) {
                        log.info("图书馆资源无法提供相关信息，切换到通用AI");
                        useGeneralAIStream(question, emitter);
                    }
                    return;
                }
                
                // 4. 创意性问题直接使用通用AI
                if (analysis.isCreative) {
                    log.info("创意性问题，直接使用通用AI");
                    useGeneralAIStream(question, emitter);
                    return;
                }
                
                // 5. 默认情况：尝试图书馆资源，失败则使用通用AI
                boolean librarySuccess = tryLibraryResourcesStream(question, emitter);
                if (!librarySuccess) {
                    log.info("图书馆资源无法提供相关信息，切换到通用AI");
                    useGeneralAIStream(question, emitter);
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
     * 尝试使用图书馆资源
     */
    private SmartQueryResponse tryLibraryResources(String question) {
        try {
            // 使用向量搜索查找相关文档
            List<DocumentChunk> relevantChunks = vectorSearchService.vectorSearch(question, 5);
            
            // 总是尝试关键词搜索作为补充
            log.info("执行关键词搜索补充");
            List<DocumentChunk> keywordChunks = performKeywordSearch(question);
            
            // 合并结果，去重，并优先排序包含关键词的文档块
            Set<String> existingIds = relevantChunks.stream()
                    .map(DocumentChunk::getId)
                    .collect(Collectors.toSet());
            
            List<DocumentChunk> keywordOnlyChunks = keywordChunks.stream()
                    .filter(chunk -> !existingIds.contains(chunk.getId()))
                    .limit(Math.max(0, 8 - relevantChunks.size())) // 最多8个结果
                    .collect(Collectors.toList());
            
            // 将关键词搜索结果放在前面，确保重要信息不会被截断
            List<DocumentChunk> finalChunks = new ArrayList<>();
            finalChunks.addAll(keywordOnlyChunks);
            finalChunks.addAll(relevantChunks);
            
            log.info("合并后总共有 {} 个文档块，其中关键词搜索贡献 {} 个", finalChunks.size(), keywordOnlyChunks.size());
            
            if (finalChunks.isEmpty()) {
                log.info("未找到相关文档");
                return null; // 返回null表示无法提供相关信息
            }
            
            // 使用RAG服务生成回答
            String ragAnswer = ragService.queryWithChunks(question, finalChunks);
            
            // 检查回答质量
            boolean isRelevant = isAnswerRelevant(ragAnswer);
            
            if (!isRelevant) {
                log.info("RAG回答质量不佳，判定为不相关");
                return null; // 返回null表示无法提供相关信息
            }
            
            // 提取文档来源 - 使用文档名称作为来源，过滤掉无效的documentId
            List<String> sources = finalChunks.stream()
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
            
            return SmartQueryResponse.builder()
                    .answer(ragAnswer)
                    .source("📚 基于图书馆资源")
                    .sourceType(SmartQueryResponse.SourceType.LIBRARY)
                    .sources(sources)
                    .relevant(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("图书馆资源查询失败", e);
            return null; // 返回null表示查询失败
        }
    }
    
    /**
     * 执行关键词搜索
     */
    private List<DocumentChunk> performKeywordSearch(String question) {
        try {
            // 提取关键词
            List<String> keywords = extractKeywords(question);
            log.info("提取的关键词: {}", keywords);
            
            List<DocumentChunk> keywordResults = new ArrayList<>();
            
            // 对每个关键词进行搜索
            for (String keyword : keywords) {
                try {
                    List<DocumentChunk> chunks = documentChunkRepository
                            .findByContentContaining(keyword, PageRequest.of(0, 3))
                            .getContent();
                    
                    // 去重添加
                    Set<String> existingIds = keywordResults.stream()
                            .map(DocumentChunk::getId)
                            .collect(Collectors.toSet());
                    
                    chunks.stream()
                            .filter(chunk -> !existingIds.contains(chunk.getId()))
                            .forEach(keywordResults::add);
                    
                    log.info("关键词 '{}' 找到 {} 个结果", keyword, chunks.size());
                    
                } catch (Exception e) {
                    log.warn("搜索关键词 '{}' 失败: {}", keyword, e.getMessage());
                }
            }
            
            log.info("关键词搜索总共找到 {} 个结果", keywordResults.size());
            return keywordResults;
            
        } catch (Exception e) {
            log.error("关键词搜索失败", e);
            return List.of();
        }
    }
    
    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String question) {
        String lowerQuestion = question.toLowerCase();
        List<String> keywords = new ArrayList<>();
        
        // MySQL相关关键词
        if (lowerQuestion.contains("mysql")) {
            keywords.add("mysql");
            keywords.add("3306");  // MySQL默认端口
        }
        
        // 端口相关关键词
        if (lowerQuestion.contains("端口") || lowerQuestion.contains("port")) {
            keywords.add("端口");
            keywords.add("port");
            keywords.add("3306");
            keywords.add("默认端口");
        }
        
        // 默认相关关键词
        if (lowerQuestion.contains("默认")) {
            keywords.add("默认");
            keywords.add("default");
        }
        
        return keywords;
    }
    
    /**
     * 使用通用AI
     */
    private SmartQueryResponse useGeneralAI(String question) {
        try {
            String prompt = String.format(
                "请回答以下问题，提供准确、有用的信息：\n\n问题：%s\n\n" +
                "请用中文回答，并保持回答的准确性和实用性。", 
                question
            );
            
            String answer = chatClient.prompt(prompt).call().content();
            
            return SmartQueryResponse.builder()
                    .answer(answer)
                    .source("🧠 基于通用知识")
                    .sourceType(SmartQueryResponse.SourceType.GENERAL)
                    .note("此回答基于AI的通用知识，建议查阅相关专业资料进行验证")
                    .relevant(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("通用AI查询失败", e);
            return SmartQueryResponse.builder()
                    .answer("抱歉，无法处理您的问题，请稍后重试。")
                    .source("🧠 基于通用知识")
                    .sourceType(SmartQueryResponse.SourceType.GENERAL)
                    .relevant(false)
                    .build();
        }
    }
    
    /**
     * 使用通用AI进行流式查询
     */
    private void useGeneralAIStream(String question, SseEmitter emitter) {
        try {
            log.info("使用通用AI进行流式查询: {}", question);
            
            // 发送开始响应
            emitter.send(StreamResponse.start("🤖 基于通用知识"));
            
            String prompt = String.format(
                "请回答以下问题，提供准确、有用的信息：\n\n问题：%s\n\n" +
                "请用中文回答，并保持回答的准确性和实用性。", 
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
     * 流式尝试使用图书馆资源
     */
    private boolean tryLibraryResourcesStream(String question, SseEmitter emitter) {
        try {
            // 使用向量搜索查找相关文档
            List<DocumentChunk> relevantChunks = vectorSearchService.vectorSearch(question, 5);
            
            // 总是尝试关键词搜索作为补充
            log.info("执行关键词搜索补充");
            List<DocumentChunk> keywordChunks = performKeywordSearch(question);
            
            // 合并结果，去重，并优先排序包含关键词的文档块
            Set<String> existingIds = relevantChunks.stream()
                    .map(DocumentChunk::getId)
                    .collect(Collectors.toSet());
            
            List<DocumentChunk> keywordOnlyChunks = keywordChunks.stream()
                    .filter(chunk -> !existingIds.contains(chunk.getId()))
                    .limit(Math.max(0, 8 - relevantChunks.size())) // 最多8个结果
                    .collect(Collectors.toList());
            
            // 将关键词搜索结果放在前面，确保重要信息不会被截断
            List<DocumentChunk> finalChunks = new ArrayList<>();
            finalChunks.addAll(keywordOnlyChunks);
            finalChunks.addAll(relevantChunks);
            
            log.info("合并后总共有 {} 个文档块，其中关键词搜索贡献 {} 个", finalChunks.size(), keywordOnlyChunks.size());
            
            if (finalChunks.isEmpty()) {
                log.info("未找到相关文档");
                return false; // 返回false表示无法提供相关信息
            }
            
            // 预检查：先用非流式方式快速生成一个简短回答，检查质量
            log.info("执行RAG回答质量预检查");
            String preCheckAnswer = ragService.queryWithChunks(question, finalChunks);
            
            if (!isAnswerRelevant(preCheckAnswer)) {
                log.info("预检查发现RAG回答不相关，切换到通用AI: {}", 
                    preCheckAnswer.substring(0, Math.min(100, preCheckAnswer.length())));
                return false; // 返回false让系统切换到通用AI
            }
            
            log.info("预检查通过，RAG回答相关，开始流式输出");
            
            // 发送开始响应（只有在预检查通过后才发送）
            emitter.send(StreamResponse.start("📚 基于图书馆资源"));
            
            // 提取文档来源 - 使用文档名称作为来源，过滤掉无效的documentId
            List<String> sources = finalChunks.stream()
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
            
            // 使用RAG服务生成流式回答，传递sources参数，让RAG服务负责完成emitter
            ragService.queryWithChunksStream(question, finalChunks, emitter, sources);
            
            return true;
                    
        } catch (Exception e) {
            log.error("图书馆资源流式查询失败", e);
            return false; // 返回false表示查询失败
        }
    }
    
    /**
     * 判断回答是否相关
     */
    private boolean isAnswerRelevant(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return false;
        }
        
        // 提取实际的回答内容（去除思考标签）
        String actualAnswer = extractActualAnswer(answer);
        String lowerAnswer = actualAnswer.toLowerCase();
        
        log.debug("原始回答长度: {}, 实际回答长度: {}", answer.length(), actualAnswer.length());
        
        // 检查明确的否定表述
        List<String> negativeIndicators = Arrays.asList(
            "无法找到相关信息", "没有找到相关信息", "未找到相关信息",
            "无法找到", "没有找到", "未找到", "找不到",
            "没有相关", "无相关", "无关信息",
            "文档中没有", "文档中未", "文档中无",
            "根据提供的文档内容，我无法",
            "根据文档内容，我无法",
            "抱歉", "无法", "不能"
        );
        
        // 如果包含明确的否定表述，认为不相关
        boolean hasNegativeIndicators = negativeIndicators.stream()
                .anyMatch(indicator -> lowerAnswer.contains(indicator));
        
        if (hasNegativeIndicators) {
            log.debug("检测到否定表述，判定为不相关: {}", actualAnswer.substring(0, Math.min(100, actualAnswer.length())));
            return false;
        }
        
        // 检查是否包含实质性内容
        // 如果回答很短且没有具体信息，可能不相关
        if (actualAnswer.length() < 30) {
            log.debug("实际回答过短，判定为不相关: {}", actualAnswer);
            return false;
        }
        
        // 检查是否包含具体的事实或数据
        boolean hasSpecificInfo = lowerAnswer.matches(".*\\d+.*") || // 包含数字
                                 lowerAnswer.contains("是") ||
                                 lowerAnswer.contains("为") ||
                                 lowerAnswer.contains("：") ||
                                 lowerAnswer.contains("。") ||
                                 lowerAnswer.contains("mysql") ||
                                 lowerAnswer.contains("数据库");
        
        log.debug("回答相关性检查结果: {}, 包含具体信息: {}", !hasNegativeIndicators && hasSpecificInfo, hasSpecificInfo);
        return hasSpecificInfo;
    }
    
    /**
     * 提取实际的回答内容（去除思考标签）
     */
    private String extractActualAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        
        // 如果包含思考标签，提取思考标签之外的内容
        if (answer.contains("<think>") && answer.contains("</think>")) {
            // 找到最后一个</think>标签的位置
            int lastThinkEndIndex = answer.lastIndexOf("</think>");
            if (lastThinkEndIndex != -1) {
                String afterThink = answer.substring(lastThinkEndIndex + 8).trim();
                if (!afterThink.isEmpty()) {
                    return afterThink;
                }
            }
            
            // 如果</think>后面没有内容，提取<think>之前的内容
            int firstThinkStartIndex = answer.indexOf("<think>");
            if (firstThinkStartIndex > 0) {
                String beforeThink = answer.substring(0, firstThinkStartIndex).trim();
                if (!beforeThink.isEmpty()) {
                    return beforeThink;
                }
            }
        }
        
        // 如果没有思考标签，返回原始内容
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