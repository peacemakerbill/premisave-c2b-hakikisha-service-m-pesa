package com.premisave.c2b_hakikisha.dto;

import com.premisave.c2b_hakikisha.model.WalletAccount;
import java.time.Instant;
import java.time.LocalDateTime;

/** Everything stored for an account in this service's MongoDB collection. */
public record SavedAccountDto(
        String userId,
        String accountNumber,
        String fullName,
        boolean frozen,
        String mpesaPhoneNumber,
        String mpesaPhoneKey,
        String pochiPhoneNumber,
        String paypalEmail,
        String paypalConnectedEmail,
        boolean stripeConnected,
        boolean stripePayoutsEnabled,
        String flutterwavePaymentMethodNetwork,
        String flutterwavePaymentMethodPhone,
        LocalDateTime createdAt,
        Instant lastSyncedAt) {

    public static SavedAccountDto from(WalletAccount a) {
        return new SavedAccountDto(
                a.getUserId(),
                a.getAccountNumber(),
                a.getFullName(),
                a.isFrozen(),
                a.getMpesaPhoneNumber(),
                a.getMpesaPhoneKey(),
                a.getPochiPhoneNumber(),
                a.getPaypalEmail(),
                a.getPaypalConnectedEmail(),
                a.isStripeConnected(),
                a.isStripePayoutsEnabled(),
                a.getFlutterwavePaymentMethodNetwork(),
                a.getFlutterwavePaymentMethodPhone(),
                a.getCreatedAt(),
                a.getLastSyncedAt());
    }
}