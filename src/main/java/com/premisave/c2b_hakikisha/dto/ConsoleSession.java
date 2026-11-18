package com.premisave.c2b_hakikisha.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** What a signed in staff member gets to use the name lookup: the paybill shortcode and a short lived access token. */
public record ConsoleSession(
        String shortcode,
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") String expiresIn) {
}
