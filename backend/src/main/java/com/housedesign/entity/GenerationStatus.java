package com.housedesign.entity;

/**
 * 生成任务状态。
 */
public enum GenerationStatus {
    /** 等待处理 */
    PENDING,
    /** 生成中 */
    PROCESSING,
    /** 成功 */
    SUCCESS,
    /** 失败 */
    FAILED
}
