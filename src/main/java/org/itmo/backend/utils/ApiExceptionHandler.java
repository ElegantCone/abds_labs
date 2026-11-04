package org.itmo.backend.utils;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);
    @ExceptionHandler(ExceptionUtils.InvalidRequestException.class)
    public ResponseEntity<?> invalidRequest(ExceptionUtils.InvalidRequestException error) {
        return ExceptionUtils.badRequest(error.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> invalidBody() {
        return ExceptionUtils.badRequest("invalid request body");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> invalidParameter(MethodArgumentTypeMismatchException error) {
        return ExceptionUtils.badRequest(ExceptionUtils.invalidParameter(error.getName()).getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> missingResource() {
        return ExceptionUtils.notFound();
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> unsupportedMethod() {
        return ResponseEntity.status(405).body(Map.of("message", "Method not allowed"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<?> unsupportedMediaType() {
        return ResponseEntity.status(415).body(Map.of("message", "Unsupported media type"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> internalError(Exception error) {
        logger.error("Request failed", error);
        return ResponseEntity.internalServerError().body(Map.of("message", "Internal server error"));
    }
}
