package com.deepfine;

import com.deepfine.error.ErrorCode;
import com.deepfine.error.GlobalException;
import com.deepfine.error.response.GlobalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


@Slf4j
@RestControllerAdvice
public class ApiControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponse<?>> handleApiException(MethodArgumentNotValidException e) {
        ErrorCode error = ErrorCode.BAD_REQUEST_BODY;
        return new ResponseEntity<>(
                GlobalResponse.error(error.name(), error.getMessage()), error.getStatus());
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<GlobalResponse<?>> handleBadRequest(Exception e) {
        ErrorCode error = ErrorCode.BAD_REQUEST_BODY;
        log.warn("잘못된 요청입니다. message={}", e.getMessage());
        return new ResponseEntity<>(
                GlobalResponse.error(error.name(), error.getMessage()), error.getStatus());
    }

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<GlobalResponse> globalApiException(GlobalException e) {
        String code = e.getErrorCode().name();
        String message = e.getErrorCode().getMessage() + " [" + e.getIdentity() + "]";
        HttpStatus status = e.getErrorCode().getStatus();
        return new ResponseEntity<>(GlobalResponse.error(code, message), status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<?>> handleException(Exception e) {
        ErrorCode error = ErrorCode.INTERNAL_SERVER_ERROR;
        log.error("예상하지 못한 서버 오류가 발생했습니다.", e);
        return new ResponseEntity<>(
                GlobalResponse.error(error.name(), error.getMessage()), error.getStatus());
    }

}
