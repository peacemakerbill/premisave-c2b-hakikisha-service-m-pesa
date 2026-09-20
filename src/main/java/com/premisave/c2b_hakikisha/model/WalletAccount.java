package com.premisave.c2b_hakikisha.model;

import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Local copy of a wallet-service account, used to answer C2B Hakikisha name lookups.
 * The document id is the lower-cased account number so lookups are case-insensitive
 * (account numbers are e-mail addresses, and customers type them in any case).
 */
@Document(collection = "wallet_accounts")
public class WalletAccount {

    @Id
    private String id;

    private String accountNumber;
    private String userId;
    private String fullName;
    private boolean frozen;

    private String mpesaPhoneNumber;
    private String pochiPhoneNumber;
    private String paypalEmail;
    private String paypalConnectedEmail;
    private boolean stripeConnected;
    private boolean stripePayoutsEnabled;
    private String flutterwavePaymentMethodNetwork;
    private String flutterwavePaymentMethodPhone;

    /** createdAt as reported by wallet-service. */
    private LocalDateTime createdAt;

    /** Start time of the sync run that last wrote this document; drives stale-record cleanup. */
    private Instant lastSyncedAt;

    public static String keyOf(String accountNumber) {
        return accountNumber == null ? null : accountNumber.trim().toLowerCase();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }
    public String getMpesaPhoneNumber() { return mpesaPhoneNumber; }
    public void setMpesaPhoneNumber(String v) { this.mpesaPhoneNumber = v; }
    public String getPochiPhoneNumber() { return pochiPhoneNumber; }
    public void setPochiPhoneNumber(String v) { this.pochiPhoneNumber = v; }
    public String getPaypalEmail() { return paypalEmail; }
    public void setPaypalEmail(String v) { this.paypalEmail = v; }
    public String getPaypalConnectedEmail() { return paypalConnectedEmail; }
    public void setPaypalConnectedEmail(String v) { this.paypalConnectedEmail = v; }
    public boolean isStripeConnected() { return stripeConnected; }
    public void setStripeConnected(boolean v) { this.stripeConnected = v; }
    public boolean isStripePayoutsEnabled() { return stripePayoutsEnabled; }
    public void setStripePayoutsEnabled(boolean v) { this.stripePayoutsEnabled = v; }
    public String getFlutterwavePaymentMethodNetwork() { return flutterwavePaymentMethodNetwork; }
    public void setFlutterwavePaymentMethodNetwork(String v) { this.flutterwavePaymentMethodNetwork = v; }
    public String getFlutterwavePaymentMethodPhone() { return flutterwavePaymentMethodPhone; }
    public void setFlutterwavePaymentMethodPhone(String v) { this.flutterwavePaymentMethodPhone = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}