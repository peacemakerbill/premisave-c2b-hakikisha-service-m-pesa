package com.premisave.c2b_hakikisha.dto;

import java.time.LocalDateTime;

/** One element of data.content returned by wallet-service GET /internal/accounts. */
public record WalletAccountDto(
        String userId,
        String accountNumber,
        String fullName,
        Boolean frozen,
        String mpesaPhoneNumber,
        String pochiPhoneNumber,
        String paypalEmail,
        String paypalConnectedEmail,
        Boolean stripeConnected,
        Boolean stripePayoutsEnabled,
        String flutterwavePaymentMethodNetwork,
        String flutterwavePaymentMethodPhone,
        LocalDateTime createdAt) {
}