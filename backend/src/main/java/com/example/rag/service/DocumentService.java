package com.example.rag.service;

import com.example.rag.dto.DocumentUploadRequest;
import com.example.rag.model.Document;
import com.example.rag.model.DocumentChunk;
import com.example.rag.model.User;
import com.example.rag.repository.DocumentRepository;
import com.example.rag.repository.DocumentChunkRepository;
import com.example.rag.service.VectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 文档服务类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final VectorSearchService vectorSearchService;
    
    @Value("${app.upload.path:./uploads}")
    private String uploadPath;
    
    @Value("${app.upload.max-size:52428800}")
    private long maxFileSize;
    
    @Value("${app.upload.allowed-types:pdf,txt,md,docx,doc}")
    private String allowedTypes;
    
    /**
     * 上传文档
     */
    @Transactional
    public Document uploadDocument(DocumentUploadRequest request, User user) {
        try {
            MultipartFile file = request.getFile();
            
            // 验证文件
            validateFile(file);
            
            // 生成文档ID和文件名
            String documentId = UUID.randomUUID().toString();
            String filename = generateFilename(file.getOriginalFilename());
            
            // 保存文件
            Path filePath = saveFile(file, filename);
            
            // 创建文档记录
            Document document = Document.builder()
                    .documentId(documentId)
                    .filename(filename)
                    .originalFilename(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .category(request.getCategory())
                    .uploadUser(user)
                    .status(Document.DocumentStatus.PROCESSING)
                    .build();
            
            document = documentRepository.save(document);
            
            // 异步处理文档（这里先标记为完成，实际应该异步处理）
            if (request.getAutoProcess()) {
                // TODO: 异步处理文档内容提取和向量化
                processDocumentAsync(document);
            }
            
            log.info("文档上传成功: {}", documentId);
            return document;
            
        } catch (Exception e) {
            log.error("文档上传失败", e);
            throw new RuntimeException("文档上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户文档列表
     */
    public Page<Document> getUserDocuments(User user, Pageable pageable) {
        return documentRepository.findByUploadUser(user, pageable);
    }
    
    /**
     * 根据ID获取文档
     */
    public Optional<Document> getDocumentById(String documentId) {
        return documentRepository.findByDocumentId(documentId);
    }
    
    /**
     * 删除文档（物理删除）
     */
    @Transactional
    public void deleteDocument(String documentId, User user) {
        Optional<Document> documentOpt = documentRepository.findByDocumentId(documentId);
        if (documentOpt.isPresent()) {
            Document document = documentOpt.get();
            
            // 检查权限
            if (!document.getUploadUser().getId().equals(user.getId())) {
                throw new RuntimeException("无权限删除此文档");
            }
            
            try {
                log.info("开始删除文档: {}", documentId);
                
                // 1. 删除向量存储中的文档
                try {
                    vectorSearchService.deleteDocumentFromVectorStore(documentId);
                    log.info("已删除向量存储中的文档: {}", documentId);
                } catch (Exception e) {
                    log.error("删除向量存储中的文档失败: {}", documentId, e);
                    // 不抛出异常，继续删除其他数据
                }
                
                // 2. 删除Elasticsearch中的文档块
                documentChunkRepository.deleteByDocumentId(documentId);
                log.info("已删除Elasticsearch中的文档块: {}", documentId);
                
                // 3. 删除物理文件
                deletePhysicalFile(document);
                
                // 4. 删除数据库记录
                documentRepository.delete(document);
                log.info("已删除数据库记录: {}", documentId);
                
                log.info("文档完整删除成功: {}", documentId);
                
            } catch (Exception e) {
                log.error("删除文档失败: {}", documentId, e);
                throw new RuntimeException("删除文档失败: " + e.getMessage());
            }
        } else {
            throw new RuntimeException("文档不存在: " + documentId);
        }
    }
    
    /**
     * 删除物理文件
     */
    private void deletePhysicalFile(Document document) {
        try {
            Path filePath = Paths.get(uploadPath, document.getFilename());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("已删除物理文件: {}", filePath);
            } else {
                log.warn("物理文件不存在，跳过删除: {}", filePath);
            }
        } catch (IOException e) {
            log.error("删除物理文件失败: {}", document.getFilename(), e);
            // 不抛出异常，允许继续删除数据库记录
        }
    }
    
    /**
     * 获取文档块
     */
    public List<DocumentChunk> getDocumentChunks(String documentId) {
        return documentChunkRepository.findByDocumentId(documentId);
    }
    
    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("文件大小超过限制");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new RuntimeException("文件名不能为空");
        }
        
        String extension = getFileExtension(filename);
        if (!isAllowedType(extension)) {
            throw new RuntimeException("不支持的文件类型: " + extension);
        }
    }
    
    /**
     * 保存文件
     */
    private Path saveFile(MultipartFile file, String filename) throws IOException {
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        
        Path filePath = uploadDir.resolve(filename);
        file.transferTo(filePath.toFile());
        return filePath;
    }
    
    /**
     * 生成文件名
     */
    private String generateFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + "." + extension;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1).toLowerCase() : "";
    }
    
    /**
     * 检查文件类型是否允许
     */
    private boolean isAllowedType(String extension) {
        return allowedTypes.contains(extension.toLowerCase());
    }
    
    /**
     * 异步处理文档
     */
    private void processDocumentAsync(Document document) {
        try {
            log.info("开始处理文档: {}", document.getDocumentId());
            
            // 1. 读取文档内容
            String content = extractDocumentContent(document);
            if (content == null || content.trim().isEmpty()) {
                log.warn("文档内容为空: {}", document.getDocumentId());
                document.setStatus(Document.DocumentStatus.FAILED);
                documentRepository.save(document);
                return;
            }
            
            // 2. 检查文本大小，防止内存溢出
            int contentLength = content.length();
            log.info("文档内容长度: {} 字符", contentLength);
            
            // 如果文本过大（超过10MB），进行预处理
            if (contentLength > 10 * 1024 * 1024) {
                log.warn("文档内容过大: {} 字符，将进行分批处理", contentLength);
                processLargeDocumentInBatches(document, content);
                return;
            }
            
            // 3. 分块处理
            List<String> chunks = splitIntoChunks(content, 1000, 200);
            
            // 4. 创建文档块并保存到Elasticsearch
            saveDocumentChunks(document, chunks);
            
            log.info("文档处理完成: {}, 生成 {} 个块", document.getDocumentId(), chunks.size());
            
        } catch (OutOfMemoryError e) {
            log.error("文档处理内存不足: {}", document.getDocumentId(), e);
            document.setStatus(Document.DocumentStatus.FAILED);
            documentRepository.save(document);
        } catch (Exception e) {
            log.error("文档处理失败: {}", document.getDocumentId(), e);
            document.setStatus(Document.DocumentStatus.FAILED);
            documentRepository.save(document);
        }
    }
    
    /**
     * 处理大文档（分批处理）
     */
    private void processLargeDocumentInBatches(Document document, String content) {
        try {
            int batchSize = 100000; // 每批处理10万字符
            int totalLength = content.length();
            int totalChunks = 0;
            List<DocumentChunk> allDocumentChunks = new java.util.ArrayList<>();
            
            for (int start = 0; start < totalLength; start += batchSize) {
                int end = Math.min(start + batchSize + 2000, totalLength); // 增加重叠
                String batch = content.substring(start, end);
                
                List<String> chunks = splitIntoChunks(batch, 1000, 200);
                
                // 调整chunk索引
                for (int i = 0; i < chunks.size(); i++) {
                    DocumentChunk documentChunk = DocumentChunk.builder()
                            .id(document.getDocumentId() + "_" + (totalChunks + i))
                            .documentId(document.getDocumentId())
                            .content(chunks.get(i))
                            .chunkIndex(totalChunks + i)
                            .startPosition(start + i * 800)
                            .endPosition(start + (i + 1) * 800)
                            .category(document.getCategory())
                            .createdAt(LocalDateTime.now().toString())
                            .updatedAt(LocalDateTime.now().toString())
                            .build();
                    
                    documentChunkRepository.save(documentChunk);
                    allDocumentChunks.add(documentChunk);
                }
                
                totalChunks += chunks.size();
                log.info("已处理批次: {}/{}, 当前块数: {}", 
                    (start / batchSize + 1), (totalLength / batchSize + 1), totalChunks);
                
                // 强制垃圾回收
                System.gc();
            }
            
            // 添加所有文档块到向量存储
            try {
                log.info("添加 {} 个文档块到向量存储", allDocumentChunks.size());
                vectorSearchService.addDocumentsToVectorStore(allDocumentChunks);
                log.info("成功添加所有文档块到向量存储");
            } catch (Exception e) {
                log.error("添加文档块到向量存储失败", e);
                // 不抛出异常，允许文档处理继续
            }
            
            // 更新文档状态
            document.setStatus(Document.DocumentStatus.COMPLETED);
            document.setChunksCount(totalChunks);
            documentRepository.save(document);
            
            log.info("大文档处理完成: {}, 生成 {} 个块", document.getDocumentId(), totalChunks);
            
        } catch (Exception e) {
            log.error("大文档处理失败: {}", document.getDocumentId(), e);
            document.setStatus(Document.DocumentStatus.FAILED);
            documentRepository.save(document);
        }
    }
    
    /**
     * 保存文档块
     */
    private void saveDocumentChunks(Document document, List<String> chunks) {
        List<DocumentChunk> documentChunks = new java.util.ArrayList<>();
        
        int chunkIndex = 0;
        for (String chunk : chunks) {
            DocumentChunk documentChunk = DocumentChunk.builder()
                    .id(document.getDocumentId() + "_" + chunkIndex)
                    .documentId(document.getDocumentId())
                    .content(chunk)
                    .chunkIndex(chunkIndex)
                    .startPosition(chunkIndex * 800) // 估算位置
                    .endPosition((chunkIndex + 1) * 800)
                    .category(document.getCategory())
                    .createdAt(LocalDateTime.now().toString())
                    .updatedAt(LocalDateTime.now().toString())
                    .build();
            
            documentChunkRepository.save(documentChunk);
            documentChunks.add(documentChunk);
            chunkIndex++;
        }
        
        // 添加到向量存储
        try {
            log.info("添加 {} 个文档块到向量存储", documentChunks.size());
            vectorSearchService.addDocumentsToVectorStore(documentChunks);
            log.info("成功添加文档块到向量存储");
        } catch (Exception e) {
            log.error("添加文档块到向量存储失败", e);
            // 不抛出异常，允许文档处理继续
        }
        
        // 更新文档状态
        document.setStatus(Document.DocumentStatus.COMPLETED);
        document.setChunksCount(chunks.size());
        documentRepository.save(document);
    }
    
    /**
     * 提取文档内容
     */
    private String extractDocumentContent(Document document) {
        try {
            Path filePath = Paths.get(uploadPath, document.getFilename());
            if (!Files.exists(filePath)) {
                log.error("文件不存在: {}", filePath);
                return null;
            }
            
            String extension = getFileExtension(document.getOriginalFilename());
            
            switch (extension.toLowerCase()) {
                case "txt":
                case "md":
                    return Files.readString(filePath);
                case "pdf":
                    return extractPdfContent(filePath);
                case "doc":
                case "docx":
                    return extractWordContent(filePath);
                default:
                    log.warn("不支持的文件类型: {}", extension);
                    return null;
            }
            
        } catch (Exception e) {
            log.error("提取文档内容失败: {}", document.getDocumentId(), e);
            return null;
        }
    }
    
    /**
     * 提取PDF内容
     */
    private String extractPdfContent(Path filePath) {
        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            log.error("提取PDF内容失败: {}", filePath, e);
            return null;
        }
    }
    
    /**
     * 提取Word内容
     */
    private String extractWordContent(Path filePath) {
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            if (filePath.toString().endsWith(".doc")) {
                WordExtractor extractor = new WordExtractor(fis);
                return extractor.getText();
            } else if (filePath.toString().endsWith(".docx")) {
                XWPFWordExtractor extractor = new XWPFWordExtractor(new XWPFDocument(fis));
                return extractor.getText();
            } else {
                log.warn("不支持的Word文件格式: {}", filePath);
                return null;
            }
        } catch (IOException e) {
            log.error("提取Word内容失败: {}", filePath, e);
            return null;
        }
    }
    
    /**
     * 将文本分块（修复版本 - 防止版权页污染）
     */
    private List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new java.util.ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        
        if (text.length() <= chunkSize) {
            chunks.add(text);
            return chunks;
        }
        
        int start = 0;
        int textLength = text.length();
        int maxIterations = (textLength / (chunkSize - overlap)) + 10; // 预估最大迭代次数
        int iteration = 0;
        
        log.debug("开始分块处理，文本长度: {}, 块大小: {}, 重叠: {}", textLength, chunkSize, overlap);
        
        while (start < textLength && iteration < maxIterations) {
            iteration++;
            int end = Math.min(start + chunkSize, textLength);
            
            // 尝试在合适的位置分割，避免截断单词或句子
            if (end < textLength) {
                // 查找最佳分割点
                int bestEnd = findBestSplitPoint(text, start + chunkSize / 2, end);
                if (bestEnd > start) {
                    end = bestEnd;
                }
            }
            
            // 使用substring创建块
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
                log.debug("创建块 {}: 起始={}, 结束={}, 长度={}", chunks.size(), start, end, chunk.length());
            }
            
            // 🔧 修复分块算法：确保合理的步长
            int nextStart = end - overlap;
            
            // 确保步长至少前进一定距离，避免无限循环
            int minStep = Math.max(50, chunkSize / 20); // 最小步长
            if (nextStart <= start) {
                nextStart = start + minStep;
                log.debug("步长过小，强制最小步长: {} -> {}", start, nextStart);
            }
            
            start = nextStart;
            
            // 如果下一个起始位置已经超出文本范围，结束循环
            if (start >= textLength) {
                break;
            }
        }
        
        // 检查是否因为达到最大迭代次数而退出
        if (iteration >= maxIterations) {
            log.warn("达到最大迭代次数 {}，强制退出分块循环", maxIterations);
        }
        
        log.info("分块完成，生成 {} 个块，迭代 {} 次", chunks.size(), iteration);
        return chunks;
    }
    
    /**
     * 查找最佳分割点
     */
    private int findBestSplitPoint(String text, int minEnd, int maxEnd) {
        // 优先级：句号 > 换行符 > 空格
        int lastPeriod = text.lastIndexOf('。', maxEnd);
        if (lastPeriod >= minEnd) {
            return lastPeriod + 1;
        }
        
        int lastExclamation = text.lastIndexOf('！', maxEnd);
        if (lastExclamation >= minEnd) {
            return lastExclamation + 1;
        }
        
        int lastQuestion = text.lastIndexOf('？', maxEnd);
        if (lastQuestion >= minEnd) {
            return lastQuestion + 1;
        }
        
        int lastNewline = text.lastIndexOf('\n', maxEnd);
        if (lastNewline >= minEnd) {
            return lastNewline + 1;
        }
        
        int lastSpace = text.lastIndexOf(' ', maxEnd);
        if (lastSpace >= minEnd) {
            return lastSpace + 1;
        }
        
        // 如果找不到合适的分割点，就在最大位置分割
        return maxEnd;
    }
} 