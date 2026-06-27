package com.document.document_intelligence_service.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OcrProcessingException.class)
    public ResponseEntity<Map<String, String>> handleOcrProcessingException(OcrProcessingException ex) {
        log.warn("OCR processing error: {}", ex.getMessage(), ex);
        Map<String, String> body = new HashMap<>();
        body.put("error", "ocr_processing_failure");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("Uploaded file is too large: {}", ex.getMessage(), ex);
        Map<String, String> body = new HashMap<>();
        body.put("error", "file_too_large");
        body.put("message", "Uploaded file exceeds the maximum allowed size.");
        return ResponseEntity.status(413).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnhandledException(Exception ex) {
        log.error("Unhandled error during document analysis", ex);
        Map<String, String> body = new HashMap<>();
        body.put("error", "internal_server_error");
        body.put("message", "An unexpected error occurred while analyzing the document.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
