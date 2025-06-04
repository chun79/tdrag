package com.example.rag.service;

import com.example.rag.dto.StreamResponse;
import com.example.rag.model.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {
    
    private final VectorSearchService vectorSearchService;
    private final ChatClient chatClient;
    
    @Value("${app.rag.similarity-threshold:0.7}")
    private double similarityThreshold;
    
    @Value("${app.rag.max-context-length:8000}")
    private int maxContextLength;
    
    @Value("${app.rag.enable-multi-round:false}")
    private boolean enableMultiRound;
    
    @Value("${app.rag.max-rounds:3}")
    private int maxRounds;
    
    private static final String RAG_PROMPT_TEMPLATE = """
            你是一个专业的AI助手。请基于以下提供的文档内容来回答用户的问题。
            
            文档内容：
            {context}
            
            用户问题：{question}
            
            请遵循以下规则：
            1. 仔细分析文档内容，包括直接陈述和间接表述
            2. 基于文档内容进行合理的推理和理解
            3. 如果文档中没有直接的相关信息，请直接基于你的通用知识给出准确的答案
            4. 不要提及"文档中没有找到"或类似的表述，直接给出有用的答案
            5. 回答要准确、简洁、有条理
            6. 如果可能，请引用具体的文档内容
            7. 使用中文回答
            8. 可以在回答前使用<think>标签展示你的思考过程，然后在</think>标签后给出正式答案
            9. **格式要求**：
               - 使用清晰的段落结构，每个要点之间用空行分隔
               - 使用序号（1. 2. 3.）或项目符号（- ）来组织列表
               - 重要概念用**粗体**标记
               - 代码或技术术语用`反引号`标记
               - 使用恰当的标点符号和换行
               - 保持逻辑清晰，结构完整
            
            **请确保你的回答格式良好、结构清晰、易于阅读。**
            
            回答：
            """;
    
    private static final String EXTRACTION_PROMPT_TEMPLATE = """
            请从以下文档内容中提取与问题相关的关键信息：
            
            文档内容：
            {context}
            
            问题：{question}
            
            请提取：
            1. 直接相关的事实和数据
            2. 间接相关的信息和线索
            3. 可能有用的背景信息
            
            如果没有相关信息，请回答"无相关信息"。
            
            提取的信息：
            """;
    
    private static final String SYNTHESIS_PROMPT_TEMPLATE = """
            基于以下提取的信息片段，请综合回答用户的问题：
            
            信息片段：
            {extracted_info}
            
            用户问题：{question}
            
            请提供一个完整、准确的回答。如果信息不足，请明确说明。
            
            回答：
            """;
    
    /**
     * 处理RAG查询
     */
    public String query(String question) {
        try {
            log.info("开始处理RAG查询: {}", question);
            
            // 1. 使用向量搜索找到相关文档
            List<DocumentChunk> relevantChunks = vectorSearchService.vectorSearch(question, 5);
            
            if (relevantChunks.isEmpty()) {
                log.info("向量搜索未找到相关文档");
                return "抱歉，我在文档中没有找到与您问题相关的信息。请尝试用不同的方式描述您的问题。";
            }
            
            log.info("向量搜索找到 {} 个相关文档块", relevantChunks.size());
            
            return generateAnswerFromChunks(question, relevantChunks);
            
        } catch (Exception e) {
            log.error("RAG查询处理失败", e);
            return "抱歉，处理您的问题时发生了错误，请稍后重试。";
        }
    }
    
    /**
     * 处理RAG查询（使用预先搜索的文档块）
     */
    public String queryWithChunks(String question, List<DocumentChunk> relevantChunks) {
        try {
            log.info("开始处理RAG查询（使用预先搜索的文档块）: {}", question);
            
            if (relevantChunks.isEmpty()) {
                log.info("未提供相关文档块");
                return "抱歉，我在文档中没有找到与您问题相关的信息。请尝试用不同的方式描述您的问题。";
            }
            
            log.info("使用 {} 个预先搜索的文档块", relevantChunks.size());
            
            // 根据配置选择处理策略
            if (enableMultiRound && relevantChunks.size() > 3) {
                return processMultiRoundQuery(question, relevantChunks);
            } else {
                return generateAnswerFromChunks(question, relevantChunks);
            }
            
        } catch (Exception e) {
            log.error("RAG查询处理失败", e);
            return "抱歉，处理您的问题时发生了错误，请稍后重试。";
        }
    }
    
    /**
     * 流式处理RAG查询（使用预先搜索的文档块）
     */
    public void queryWithChunksStream(String question, List<DocumentChunk> relevantChunks, SseEmitter emitter) {
        queryWithChunksStream(question, relevantChunks, emitter, null);
    }
    
    /**
     * 流式处理RAG查询（使用预先搜索的文档块，带来源信息）
     */
    public void queryWithChunksStream(String question, List<DocumentChunk> relevantChunks, SseEmitter emitter, List<String> sources) {
        try {
            log.info("开始快速流式处理RAG查询（使用预先搜索的文档块）: {}", question);
            
            if (relevantChunks.isEmpty()) {
                log.info("未提供相关文档块");
                emitter.send(StreamResponse.chunk("抱歉，我在文档中没有找到与您问题相关的信息。请尝试用不同的方式描述您的问题。"));
                if (sources != null) {
                    emitter.send(StreamResponse.source(sources));
                }
                emitter.send(StreamResponse.end());
                emitter.complete();
                return;
            }
            
            log.info("使用 {} 个预先搜索的文档块进行快速流式处理", relevantChunks.size());
            
            // 直接构建上下文，不进行多轮查询
            String context = buildContextFast(relevantChunks);
            
            // 生成流式回答
            generateResponseStream(question, context, emitter, sources);
            
        } catch (Exception e) {
            log.error("快速流式RAG查询处理失败", e);
            try {
                emitter.send(StreamResponse.error("抱歉，处理您的问题时发生了错误，请稍后重试。"));
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
     * 多轮查询处理
     */
    private String processMultiRoundQuery(String question, List<DocumentChunk> chunks) {
        try {
            log.info("开始多轮查询处理，文档块数量: {}", chunks.size());
            
            // 第一轮：信息提取
            StringBuilder extractedInfo = new StringBuilder();
            int chunkSize = Math.min(3, chunks.size()); // 每轮处理3个文档块
            
            for (int round = 0; round < maxRounds && round * chunkSize < chunks.size(); round++) {
                int start = round * chunkSize;
                int end = Math.min(start + chunkSize, chunks.size());
                List<DocumentChunk> roundChunks = chunks.subList(start, end);
                
                log.info("第 {} 轮信息提取，处理文档块 {}-{}", round + 1, start, end - 1);
                
                String roundContext = buildContext(roundChunks);
                String extractedRoundInfo = extractInformation(question, roundContext);
                
                if (!extractedRoundInfo.contains("无相关信息")) {
                    extractedInfo.append("片段 ").append(round + 1).append("：\n")
                               .append(extractedRoundInfo).append("\n\n");
                }
            }
            
            // 第二轮：信息综合
            if (extractedInfo.length() > 0) {
                log.info("开始信息综合，提取的信息长度: {} 字符", extractedInfo.length());
                return synthesizeAnswer(question, extractedInfo.toString());
            } else {
                log.info("未提取到相关信息，回退到单轮处理");
                return generateAnswerFromChunks(question, chunks.subList(0, Math.min(3, chunks.size())));
            }
            
        } catch (Exception e) {
            log.error("多轮查询处理失败", e);
            // 回退到单轮处理
            return generateAnswerFromChunks(question, chunks);
        }
    }
    
    /**
     * 信息提取
     */
    private String extractInformation(String question, String context) {
        try {
            PromptTemplate promptTemplate = new PromptTemplate(EXTRACTION_PROMPT_TEMPLATE);
            Prompt prompt = promptTemplate.create(Map.of(
                    "question", question,
                    "context", context
            ));
            
            String extracted = chatClient.prompt(prompt).call().content();
            log.debug("提取的信息: {}", extracted);
            return extracted;
            
        } catch (Exception e) {
            log.error("信息提取失败", e);
            return "无相关信息";
        }
    }
    
    /**
     * 信息综合
     */
    private String synthesizeAnswer(String question, String extractedInfo) {
        try {
            PromptTemplate promptTemplate = new PromptTemplate(SYNTHESIS_PROMPT_TEMPLATE);
            Prompt prompt = promptTemplate.create(Map.of(
                    "question", question,
                    "extracted_info", extractedInfo
            ));
            
            String answer = chatClient.prompt(prompt).call().content();
            log.info("综合回答生成完成，长度: {} 字符", answer.length());
            return answer;
            
        } catch (Exception e) {
            log.error("信息综合失败", e);
            return "抱歉，综合信息时发生了错误。";
        }
    }
    
    /**
     * 从文档块生成回答
     */
    private String generateAnswerFromChunks(String question, List<DocumentChunk> relevantChunks) {
        // 2. 构建上下文
        String context = buildContext(relevantChunks);
        
        // 3. 生成AI回答
        String response = generateResponse(question, context);
        
        log.info("RAG查询处理完成");
        return response;
    }
    
    /**
     * 快速构建上下文（性能优化版本）
     */
    private String buildContextFast(List<DocumentChunk> chunks) {
        StringBuilder contextBuilder = new StringBuilder();
        int currentLength = 0;
        int fastMaxLength = 3000; // 固定使用较小的上下文长度以提高速度
        
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
     * 构建上下文
     */
    private String buildContext(List<DocumentChunk> chunks) {
        StringBuilder contextBuilder = new StringBuilder();
        int currentLength = 0;
        int actualMaxLength = calculateDynamicMaxLength(chunks.size());
        
        log.info("动态计算的最大上下文长度: {} 字符", actualMaxLength);
        
        for (DocumentChunk chunk : chunks) {
            String chunkContent = chunk.getContent();
            
            // 检查是否超过最大上下文长度
            if (currentLength + chunkContent.length() > actualMaxLength) {
                // 截取部分内容
                int remainingLength = actualMaxLength - currentLength;
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
        log.info("构建的上下文长度: {} 字符", context.length());
        
        return context;
    }
    
    /**
     * 动态计算最大上下文长度
     */
    private int calculateDynamicMaxLength(int chunkCount) {
        // 基础长度
        int baseLength = maxContextLength;
        
        // 根据文档块数量调整
        if (chunkCount <= 3) {
            return baseLength; // 少量文档块，使用完整长度
        } else if (chunkCount <= 5) {
            return (int) (baseLength * 1.2); // 中等数量，稍微增加
        } else {
            return (int) (baseLength * 1.5); // 大量文档块，显著增加
        }
    }
    
    /**
     * 过滤内容，去掉其他不需要的内容（保留思考标签给前端处理）
     */
    private String filterContent(String content) {
        if (content == null) {
            return "";
        }
        
        // 不再过滤思考标签，让前端自行处理
        // 只过滤其他不需要的内容
        String filtered = content;
        
        // 过滤掉其他可能的思考标记（非标准格式）
        filtered = filtered.replaceAll("\\[思考\\].*?\\[/思考\\]", "");
        filtered = filtered.replaceAll("\\[思考\\]", "");
        filtered = filtered.replaceAll("\\[/思考\\]", "");
        
        // 过滤掉空的思考内容
        if (filtered.trim().equals("思考：") || filtered.trim().equals("思考:")) {
            return "";
        }
        
        // 过滤掉纯换行符
        if (filtered.trim().isEmpty() || filtered.equals("\n")) {
            return "";
        }
        
        return filtered;
    }
    
    /**
     * 后处理完整响应（保留思考标签）
     */
    private String postProcessResponse(String response) {
        if (response == null) {
            return "";
        }
        
        // 不再过滤思考标签，保持原始内容
        // 只清理多余的空行
        String filtered = response.replaceAll("\n{3,}", "\n\n");
        
        return filtered.trim();
    }
    
    /**
     * 生成AI回答
     */
    private String generateResponse(String question, String context) {
        try {
            PromptTemplate promptTemplate = new PromptTemplate(RAG_PROMPT_TEMPLATE);
            Prompt prompt = promptTemplate.create(Map.of(
                    "question", question,
                    "context", context
            ));
            
            log.info("发送提示到AI模型");
            String response = chatClient.prompt(prompt).call().content();
            
            log.info("AI模型响应长度: {} 字符", response.length());
            
            // 后处理响应，过滤思考标签
            String filteredResponse = postProcessResponse(response);
            log.info("过滤后响应长度: {} 字符", filteredResponse.length());
            
            return filteredResponse;
            
        } catch (Exception e) {
            log.error("生成AI回答失败", e);
            return "抱歉，生成回答时发生了错误。";
        }
    }
    
    /**
     * 生成流式AI回答
     */
    private void generateResponseStream(String question, String context, SseEmitter emitter, List<String> sources) {
        try {
            PromptTemplate promptTemplate = new PromptTemplate(RAG_PROMPT_TEMPLATE);
            Prompt prompt = promptTemplate.create(Map.of(
                    "question", question,
                    "context", context
            ));
            
            log.info("发送流式提示到AI模型");
            
            // 添加计数器和时间戳来验证流式行为
            final long startTime = System.currentTimeMillis();
            final java.util.concurrent.atomic.AtomicInteger chunkCount = new java.util.concurrent.atomic.AtomicInteger(0);
            final java.util.concurrent.atomic.AtomicInteger sentCount = new java.util.concurrent.atomic.AtomicInteger(0);
            
            // 使用流式调用
            chatClient.prompt(prompt).stream().content()
                .doOnNext(chunk -> {
                    try {
                        int currentChunk = chunkCount.incrementAndGet();
                        long elapsed = System.currentTimeMillis() - startTime;
                        
                        log.info("🚀 接收到第 {} 个chunk ({}ms): [{}]", currentChunk, elapsed, 
                                chunk.length() > 20 ? chunk.substring(0, 20) + "..." : chunk);
                        
                        // 过滤掉思考标签和不需要的内容
                        String filteredChunk = filterContent(chunk);
                        
                        if (!filteredChunk.isEmpty()) {
                            int sentChunk = sentCount.incrementAndGet();
                            log.info("📤 发送第 {} 个过滤后chunk ({}ms): [{}]", sentChunk, elapsed, 
                                    filteredChunk.length() > 20 ? filteredChunk.substring(0, 20) + "..." : filteredChunk);
                            emitter.send(StreamResponse.chunk(filteredChunk));
                        } else {
                            log.info("🚫 第 {} 个chunk被过滤掉 ({}ms)", currentChunk, elapsed);
                        }
                    } catch (IOException e) {
                        log.error("发送流式内容失败", e);
                    }
                })
                .doOnComplete(() -> {
                    try {
                        long totalTime = System.currentTimeMillis() - startTime;
                        log.info("✅ 流式AI回答生成完成 - 总时间: {}ms, 接收chunks: {}, 发送chunks: {}", 
                                totalTime, chunkCount.get(), sentCount.get());
                        
                        // 发送来源信息和结束事件
                        if (sources != null) {
                            emitter.send(StreamResponse.source(sources));
                        }
                        emitter.send(StreamResponse.end());
                        emitter.complete();
                    } catch (IOException e) {
                        log.error("完成流式响应失败", e);
                        emitter.completeWithError(e);
                    }
                })
                .doOnError(error -> {
                    log.error("生成流式AI回答失败", error);
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
            log.error("生成流式AI回答失败", e);
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
     * 单轮查询处理（快速响应）
     */
    public String queryWithChunksSingleRound(String question, List<DocumentChunk> relevantChunks) {
        try {
            log.info("开始单轮RAG查询处理: {}", question);
            
            if (relevantChunks.isEmpty()) {
                log.info("未提供相关文档块");
                return "抱歉，我在文档中没有找到与您问题相关的信息。请尝试用不同的方式描述您的问题。";
            }
            
            log.info("使用 {} 个文档块进行单轮查询", relevantChunks.size());
            
            // 直接使用单轮处理，不进行多轮查询
            return generateAnswerFromChunks(question, relevantChunks);
            
        } catch (Exception e) {
            log.error("单轮RAG查询处理失败", e);
            return "抱歉，处理您的问题时发生了错误，请稍后重试。";
        }
    }
} 