package com.premisave.c2b_hakikisha.exception;

import org.springframework.http.HttpStatus;

/** Authentication problems; rendered as {"errorCode","errorMessage"}. */
@SuppressWarnings("serial")
public class AuthException extends RuntimeException {

    private final HttpStatus status;

    public AuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}