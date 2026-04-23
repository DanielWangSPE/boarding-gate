package com.boardinggate.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应外壳。
 * <p>
 * 关键约定（与 design-docs/coding/01-认证与会话管理/01-01-登录认证.spec.md §2 一致）：
 * code 为<strong>字符串</strong>，成功固定为 "200"，失败为 "A0210" 等业务码，
 * 前端使用 {@code code === '200'} 判断成功，切勿与数字 200 比较。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    public static final String SUCCESS_CODE = "200";
    public static final String SUCCESS_MESSAGE = "ok";

    private String code;
    private String message;
    private T data;
    private Object error;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, data, null);
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> fail(ApiCode apiCode) {
        return fail(apiCode.getCode(), apiCode.getMessage(), null);
    }

    public static <T> ApiResponse<T> fail(ApiCode apiCode, T data) {
        return new ApiResponse<>(apiCode.getCode(), apiCode.getMessage(), data, null);
    }

    public static <T> ApiResponse<T> fail(String code, String message, Object error) {
        return new ApiResponse<>(code, message, null, error);
    }
}
