package group2.CSAT.BMTT.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

/**
 * GlobalExceptionHandler bắt mọi lỗi RuntimeException
 * và trả về HTTP 400 kèm message, thay vì quăng lỗi 500 Internal Server Error làm sập Swing UI.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {
        FieldError firstError = e.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = firstError != null
                ? firstError.getField() + ": " + firstError.getDefaultMessage()
                : "Du lieu request khong hop le.";

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleBadJson(HttpMessageNotReadableException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "JSON request khong hop le hoac co field khong duoc ho tro."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        // In log ra console backend để dễ debug
        e.printStackTrace();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Lỗi hệ thống không xác định."));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        e.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Lỗi server: " + e.getMessage()));
    }
}
