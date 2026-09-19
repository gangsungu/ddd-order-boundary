package com.roykhan.dddorderboundary.common.exception;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {
    HttpStatus getHttpStatus();
    String getMessage();
    String name();

    default BusinessException exception() {
        return new BusinessException(this);
    }

    default BusinessException exception(String message) {
        return new BusinessException(this, message);
    }

    default BusinessException exception(String message, Object data) {
        return new BusinessException(this, message, data);
    }
}
