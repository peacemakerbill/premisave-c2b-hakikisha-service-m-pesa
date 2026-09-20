package com.premisave.c2b_hakikisha.dto;

/** Error body of the token endpoint / bearer check: {"errorCode":"401","errorMessage":"..."}. */
public record AuthErrorResponse(String errorCode, String errorMessage) {
}