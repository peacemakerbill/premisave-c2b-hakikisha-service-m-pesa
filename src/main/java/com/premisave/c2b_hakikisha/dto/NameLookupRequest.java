package com.premisave.c2b_hakikisha.dto;

/**
 * Every field is a String on purpose: Safaricom's doc lists shortcode as an integer but its own
 * sample sends it as a string, and timestamp may be a UNIX value or ISO 8601. Jackson coerces
 * JSON numbers to String, so both forms are accepted.
 */
public record NameLookupRequest(
        String requestId,
        String timestamp,
        String accountNumber,
        String shortcode) {
}