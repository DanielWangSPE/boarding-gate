package com.boardinggate.web.exception;

import com.boardinggate.web.dto.ApiCode;
import com.boardinggate.web.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理：
 * <ul>
 *   <li>{@link BusinessException} → HTTP 200 + 业务码</li>
 *   <li>参数校验失败 → HTTP 200 + A0400</li>
 *   <li>其它 → HTTP 200 + B0500（记录异常栈）</li>
 * </ul>
 * 统一使用 HTTP 200 + 业务 code 的模式，与 spec §2 一致，避免和 HTTP 状态码混淆。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException ex) {
        ApiResponse<Object> body = new ApiResponse<>(
                ex.getApiCode().getCode(),
                ex.getMessage(),
                ex.getData(),
                null);
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ResponseEntity.ok(ApiResponse.fail(ApiCode.PARAM_INVALID.getCode(), msg, null));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Object>> handleBind(BindException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ResponseEntity.ok(ApiResponse.fail(ApiCode.PARAM_INVALID.getCode(), msg, null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraint(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(this::formatViolation)
                .collect(Collectors.joining("; "));
        return ResponseEntity.ok(ApiResponse.fail(ApiCode.PARAM_INVALID.getCode(), msg, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnknown(Exception ex) {
        log.error("未捕获的服务端异常", ex);
        return ResponseEntity.ok(ApiResponse.fail(ApiCode.INTERNAL_ERROR));
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }

    private String formatViolation(ConstraintViolation<?> cv) {
        return cv.getPropertyPath() + ": " + cv.getMessage();
    }
}
