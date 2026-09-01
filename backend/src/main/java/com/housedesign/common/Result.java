package com.housedesign.common;

import lombok.Data;

/**
 * 统一响应包装类，所有接口返回均以此结构包裹，便于前端统一处理。
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> {

    /** 业务码，200 表示成功，非 200 表示失败。 */
    private int code;
    /** 提示信息，失败时含具体原因。 */
    private String message;
    /** 业务数据，失败时通常为 null。 */
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功响应（带数据）。 */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /** 成功响应（无数据）。 */
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    /** 失败响应（带错误码与消息）。 */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
