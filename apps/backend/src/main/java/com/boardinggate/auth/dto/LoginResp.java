package com.boardinggate.auth.dto;

import lombok.Builder;
import lombok.Data;

/**
 * POST /auth/crypto/login 响应体 data 部分。
 */
@Data
@Builder
public class LoginResp {

    private String accessToken;
    private Integer expiresIn;
    private String tokenType;
    private Boolean forceChangePassword;
}
