package com.querySense.common;

import com.querySense.safety.UnsafeSqlException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataAccessException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1) Unsafe SQL caught by our validator → 400 Bad Request
    @ExceptionHandler(UnsafeSqlException.class)
    public ResponseEntity<Map<String, String>> handleUnsafeSql(UnsafeSqlException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    // 2) Empty/blank question (the @NotBlank rule) → 400 Bad Request
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getDefaultMessage())
                .orElse("Invalid request.");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", message));
    }

    // 3) The generated SQL failed at the database (e.g. unknown column) → 400
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDbError(DataAccessException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error",
                        "The generated query could not be executed against the database."));
    }

    // 4) Anything unexpected → 500 with a generic message (no stack trace leaked)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleOther(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Something went wrong processing your request."));
    }
}