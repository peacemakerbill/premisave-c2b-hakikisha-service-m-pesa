package com.premisave.c2b_hakikisha.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") String expiresIn) {
}