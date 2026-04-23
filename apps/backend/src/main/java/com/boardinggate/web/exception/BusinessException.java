package com.boardinggate.web.exception;

import com.boardinggate.web.dto.ApiCode;
import lombok.Getter;

/**
 * 业务异常：携带 {@link ApiCode}，由全局异常处理器转成统一外壳响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ApiCode apiCode;
    private final transient Object data;

    public BusinessException(ApiCode apiCode) {
        super(apiCode.getMessage());
        this.apiCode = apiCode;
        this.data = null;
    }

    public BusinessException(ApiCode apiCode, Object data) {
        super(apiCode.getMessage());
        this.apiCode = apiCode;
        this.data = data;
    }

    public BusinessException(ApiCode apiCode, String customMessage) {
        super(customMessage);
        this.apiCode = apiCode;
        this.data = null;
    }
}
