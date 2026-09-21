package com.premisave.c2b_hakikisha.dto;

import java.time.LocalDateTime;

/** Same envelope the wallet service uses: {success, message, data, timestamp}. */
public record ApiEnvelope<T>(boolean success, String message, T data, String timestamp) {

    public static <T> ApiEnvelope<T> ok(String message, T data) {
        return new ApiEnvelope<>(true, message, data, LocalDateTime.now().toString());
    }
}