package com.boardinggate.auth.dto;

import lombok.Builder;
import lombok.Data;

/**
 * {@code POST /auth/refresh} 响应体 data 部分。
 */
@Data
@Builder
public class RefreshResp {

    private String accessToken;

    private Integer expiresIn;

    private String tokenType;
}
