package com.roykhan.dddorderboundary.common.exception;

import com.roykhan.dddorderboundary.common.response.ApiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 처리되지 않은 모든 예외의 최종 방어선.
     *
     * <p>여기에 걸린다는 것은 예상하지 못한 오류라는 뜻이므로 스택트레이스를 남긴다.
     * 스프링은 예외 타입 계층에서 가장 구체적인 핸들러를 선택하므로,
     * 아래의 개별 핸들러가 먼저 적용되고 남은 것만 이 메서드로 온다.
     */
    @ExceptionHandler
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error(e.getMessage(), e);
        return toResponse(CommonErrorCode.INTERNAL_SERVER_ERROR, null);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        log.warn("code={}, message={}", e.getErrorCode().name(), e.getMessage());
        return toResponse(e.getErrorCode(), e.getMessage(), e.getData());
    }

    /**
     * {@code @Valid} 검증 실패. 어느 필드가 왜 거절됐는지 data에 담아 돌려준다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        log.warn("검증 실패: {}", fieldErrors);
        return toResponse(CommonErrorCode.VALIDATION_FAILED, fieldErrors);
    }

    /**
     * 본문을 읽을 수 없는 경우. 깨진 JSON, 타입이 맞지 않는 값 등.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotReadableException(HttpMessageNotReadableException e) {
        log.warn("본문을 읽을 수 없음: {}", e.getMessage());
        return toResponse(CommonErrorCode.INVALID_REQUEST, null);
    }

    /**
     * 경로 변수나 쿼리 파라미터의 타입이 맞지 않는 경우. 예) /api/product/abc
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("파라미터 타입 불일치: name={}, value={}", e.getName(), e.getValue());
        return toResponse(CommonErrorCode.INVALID_REQUEST, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("존재하지 않는 경로: {}", e.getResourcePath());
        return toResponse(CommonErrorCode.NOT_FOUND, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("지원하지 않는 메서드: {}", e.getMethod());
        return toResponse(CommonErrorCode.METHOD_NOT_ALLOWED, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("지원하지 않는 미디어 타입: {}", e.getContentType());
        return toResponse(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE, null);
    }

    private ResponseEntity<ApiResponse<Object>> toResponse(BaseErrorCode errorCode, Object data) {
        return toResponse(errorCode, errorCode.getMessage(), data);
    }

    private ResponseEntity<ApiResponse<Object>> toResponse(BaseErrorCode errorCode, String message, Object data) {
        ApiResponse<Object> response = ApiResponse.failure(errorCode.name(), message, data);
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }
}
