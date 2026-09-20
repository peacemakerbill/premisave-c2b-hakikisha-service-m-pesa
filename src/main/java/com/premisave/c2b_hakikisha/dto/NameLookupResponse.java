package com.premisave.c2b_hakikisha.dto;

public record NameLookupResponse(
        String requestId,
        String timestamp,
        String accountName,
        String accountNumber,
        String shortcode) {
}