package com.housedesign.common;

import lombok.Getter;

/**
 * 业务异常：用于抛出可预期的业务错误（如资源不存在、参数非法）。
 * 由 {@link GlobalExceptionHandler} 统一捕获并转换为统一响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码，默认 400；可自定义（如 404）。 */
    private final int code;

    /** 使用默认错误码 400 构造异常。 */
    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    /** 使用自定义错误码构造异常。 */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
