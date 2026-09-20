package com.premisave.c2b_hakikisha.dto;

/** Error body of the name-lookup endpoint: {"requestId":"...","errorMessage":"..."}. */
public record ErrorResponse(String requestId, String errorMessage) {
}