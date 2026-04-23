package com.boardinggate.web.dto;

/**
 * 业务错误码枚举（与 spec 中表格一一对应）。
 * A02xx 段为认证与会话管理相关错误。
 */
public enum ApiCode {

    /** 用户名或密码错误 */
    LOGIN_BAD_CREDENTIALS("A0210", "用户名或密码错误"),
    /** 账号已停用 */
    LOGIN_ACCOUNT_DISABLED("A0212", "账号已停用，请联系管理员"),
    /** 登录加密参数无效、已过期或已使用 */
    LOGIN_CRYPTO_INVALID("A0213", "登录加密参数无效、已过期或已使用"),
    /** 服务端暂无法签发加密参数 */
    CRYPTO_ISSUE_FAILED("A0215", "服务端暂无法签发加密参数，请稍后重试"),
    /** 需要强制修改密码（登录成功但需走改密流程） */
    LOGIN_FORCE_CHANGE_PASSWORD("A0220", "需要强制修改密码"),

    /** Refresh Token 无效或已过期 */
    REFRESH_TOKEN_INVALID("A0230", "Refresh Token 无效或已过期，需重新登录"),
    /** Refresh Token 已被吊销 */
    REFRESH_TOKEN_REVOKED("A0231", "Refresh Token 已被吊销"),
    /** Access Token 无效或已过期 */
    ACCESS_TOKEN_INVALID("A0232", "Access Token 无效或已过期"),

    /** 未登录或凭证缺失 */
    UNAUTHENTICATED("A0240", "未登录或凭证缺失"),

    /** 参数校验失败 */
    PARAM_INVALID("A0400", "请求参数不合法"),
    /** 服务器内部错误 */
    INTERNAL_ERROR("B0500", "服务器内部错误，请稍后重试");

    private final String code;
    private final String message;

    ApiCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
