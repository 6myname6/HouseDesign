package com.housedesign.service.ai;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

/**
 * AI 生成输入上下文：聚合一次生成所需的项目信息与设计图来源。
 */
@Data
@Builder
public class GenerationContext {

    /** 项目 ID。 */
    private Long projectId;
    /** 项目名称。 */
    private String projectName;
    /** 装修风格 code，见 com.housedesign.entity.DesignStyle。 */
    private String style;
    /** 设计图在服务器上的绝对路径（本地文件读取用）。 */
    private Path imageAbsolutePath;
    /** 设计图可访问 URL（作为预览图兜底）。 */
    private String imageUrl;
}
