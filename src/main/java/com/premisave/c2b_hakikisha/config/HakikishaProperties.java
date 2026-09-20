package com.premisave.c2b_hakikisha.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Binds the {@code hakikisha.*} block of application.yml. Startup fails fast if a required value is missing. */
@Validated
@ConfigurationProperties(prefix = "hakikisha")
public record HakikishaProperties(
        @NotBlank String shortcode,
        @Valid @NotNull Auth auth,
        @Valid @NotNull Sync sync) {

    public record Auth(
            @NotBlank String username,
            @NotBlank String password,
            @Min(1) long tokenTtlSeconds) {
    }

    public record Sync(
            boolean enabled,
            @Min(1) int intervalMinutes,
            boolean runOnStartup,
            @Min(1) int pageSize,
            boolean deleteStale,
            @Min(1) int connectTimeoutSeconds,
            @Min(1) int readTimeoutSeconds) {
    }
}