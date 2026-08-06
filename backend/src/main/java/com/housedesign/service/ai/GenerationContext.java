package com.housedesign.service.ai;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

/**
 * AI 生成输入上下文。
 */
@Data
@Builder
public class GenerationContext {

    private Long projectId;
    private String projectName;
    /** 装修风格 code，见 com.housedesign.entity.DesignStyle */
    private String style;
    /** 设计图在服务器上的绝对路径 */
    private Path imageAbsolutePath;
    /** 设计图可访问 URL */
    private String imageUrl;
}
