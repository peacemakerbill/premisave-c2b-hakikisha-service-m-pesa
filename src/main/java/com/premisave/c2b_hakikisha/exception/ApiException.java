package com.premisave.c2b_hakikisha.exception;

import org.springframework.http.HttpStatus;

/** Name-lookup problems; rendered as {"requestId","errorMessage"}. */
@SuppressWarnings("serial")
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String requestId;

    public ApiException(HttpStatus status, String requestId, String message) {
        super(message);
        this.status = status;
        this.requestId = requestId;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getRequestId() {
        return requestId;
    }
}