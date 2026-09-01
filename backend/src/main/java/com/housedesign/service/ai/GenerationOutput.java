package com.housedesign.service.ai;

import lombok.Builder;
import lombok.Data;

/**
 * AI 生成输出结果：承载一次生成产出的各类 URL 与场景配置。
 */
@Data
@Builder
public class GenerationOutput {

    /** 实际使用的 provider 标识。 */
    private String provider;
    /** 生成的 3D 模型（glb/gltf）URL，可为空（程序化场景时为 null）。 */
    private String modelUrl;
    /** 预览图 URL，可为空。 */
    private String previewImageUrl;
    /** 全景图（等距柱状）URL，可为空。 */
    private String panoramaUrl;
    /** 前端程序化渲染用的场景配置 JSON，可为空。 */
    private String sceneConfig;
}
