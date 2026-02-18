package com.blog.user.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorBody> handleBusiness(BusinessException e) {
        ErrorBody body = new ErrorBody(e.getMessage(), e.getCode());
        return ResponseEntity.status(e.getHttpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        ErrorBody body = new ErrorBody(msg, "BAD_REQUEST");
        return ResponseEntity.status(400).body(body);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorBody> handleDataAccess(DataAccessException e) {
        e.printStackTrace();
        String msg = e.getMostSpecificCause().getMessage();
        if (msg == null) msg = "数据库操作失败";
        ErrorBody body = new ErrorBody("数据库错误: " + msg, "DATA_ACCESS_ERROR");
        return ResponseEntity.status(500).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleOther(Exception e) {
        e.printStackTrace();
        ErrorBody body = new ErrorBody("服务端错误", "INTERNAL_ERROR");
        return ResponseEntity.status(500).body(body);
    }

    public record ErrorBody(String message, String code) {}
}
