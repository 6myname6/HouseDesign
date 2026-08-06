package com.housedesign.service.ai;

/**
 * 图片 -> 3D 施工效果 生成服务抽象。
 * 不同 provider（mock / meshy / tripo / custom）提供不同实现。
 */
public interface ImageTo3DService {

    /** 该实现对应的 provider 标识 */
    String provider();

    /**
     * 根据设计图生成 3D 施工效果结果。
     *
     * @throws Exception 生成失败时抛出，由上层捕获并标记任务失败
     */
    GenerationOutput generate(GenerationContext context) throws Exception;
}
