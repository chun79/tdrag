package com.example.rag.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 文档上传请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUploadRequest {
    
    @NotNull(message = "文件不能为空")
    private MultipartFile file;
    
    @Size(max = 50, message = "分类名称不能超过50字符")
    @Builder.Default
    private String category = "general";
    
    @Size(max = 500, message = "描述不能超过500字符")
    private String description;
    
    @Builder.Default
    private Boolean autoProcess = true;
} 