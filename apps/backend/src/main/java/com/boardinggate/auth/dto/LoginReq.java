package com.boardinggate.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * POST /auth/crypto/login 请求体。
 */
@Data
public class LoginReq {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过 64")
    private String username;

    @NotBlank(message = "cryptoId 不能为空")
    private String cryptoId;

    /** SM4-CBC/PKCS7 密文的 Hex 字符串 */
    @NotBlank(message = "password 不能为空")
    private String password;
}
