package com.conk.wms.common.controller;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
// 컨트롤러마다 흩어져 있던 예외 응답 포맷을 한 곳에서 통일한다.
// 현재는 BusinessException만 우선 공통 처리하고, 이후 다른 예외 타입도 여기로 확장할 수 있다.
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.failure(errorCode.getCode(), ex.getMessage()));
    }
}
