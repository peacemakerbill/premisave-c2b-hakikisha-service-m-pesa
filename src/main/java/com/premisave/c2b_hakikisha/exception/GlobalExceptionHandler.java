package com.premisave.c2b_hakikisha.exception;

import com.premisave.c2b_hakikisha.dto.AuthErrorResponse;
import com.premisave.c2b_hakikisha.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<AuthErrorResponse> handleAuth(AuthException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new AuthErrorResponse(String.valueOf(ex.getStatus().value()), ex.getMessage()));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponse(ex.getRequestId(), ex.getMessage()));
    }

    /** Missing or unparsable JSON body -> 422, as the doc specifies for missing/malformed fields. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(new ErrorResponse(null, "Missing required fields"));
    }
}