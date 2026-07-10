package com.ink.common.utils;

import com.ink.common.constant.ApiCodeConstants;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 统一响应结果
 */
@Data
@Accessors(chain = true)
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;
    private long timestamp;

    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> success() {
        return new Result<T>()
                .setCode(ApiCodeConstants.SUCCESS)
                .setMessage("操作成功");
    }

    public static <T> Result<T> success(T data) {
        return new Result<T>()
                .setCode(ApiCodeConstants.SUCCESS)
                .setMessage("操作成功")
                .setData(data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<T>()
                .setCode(ApiCodeConstants.SUCCESS)
                .setMessage(message)
                .setData(data);
    }

    public static <T> Result<T> error() {
        return new Result<T>()
                .setCode(ApiCodeConstants.INTERNAL_ERROR)
                .setMessage("操作失败");
    }

    public static <T> Result<T> error(String message) {
        return new Result<T>()
                .setCode(ApiCodeConstants.INTERNAL_ERROR)
                .setMessage(message);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<T>()
                .setCode(code)
                .setMessage(message);
    }

    public static <T> Result<T> badRequest(String message) {
        return new Result<T>()
                .setCode(400)
                .setMessage(message);
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<T>()
                .setCode(ApiCodeConstants.UNAUTHORIZED)
                .setMessage(message);
    }

    public static <T> Result<T> forbidden(String message) {
        return new Result<T>()
                .setCode(ApiCodeConstants.FORBIDDEN)
                .setMessage(message);
    }

    public static <T> Result<T> notFound(String message) {
        return new Result<T>()
                .setCode(ApiCodeConstants.NOT_FOUND)
                .setMessage(message);
    }
}
