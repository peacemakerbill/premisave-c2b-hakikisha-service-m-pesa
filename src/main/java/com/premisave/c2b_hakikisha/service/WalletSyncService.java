package com.premisave.c2b_hakikisha.service;

import com.premisave.c2b_hakikisha.client.WalletServiceClient;
import com.premisave.c2b_hakikisha.config.HakikishaProperties;
import com.premisave.c2b_hakikisha.dto.WalletAccountDto;
import com.premisave.c2b_hakikisha.dto.WalletAccountsResponse;
import com.premisave.c2b_hakikisha.model.WalletAccount;
import com.premisave.c2b_hakikisha.repository.WalletAccountRepository;
import com.premisave.c2b_hakikisha.util.PhoneNumbers;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Pulls every page of accounts from wallet-service and upserts them into MongoDB. */
@Service
public class WalletSyncService {

    private static final Logger log = LoggerFactory.getLogger(WalletSyncService.class);

    private final WalletServiceClient walletClient;
    private final WalletAccountRepository repository;
    private final HakikishaProperties props;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public WalletSyncService(WalletServiceClient walletClient,
                             WalletAccountRepository repository,
                             HakikishaProperties props) {
        this.walletClient = walletClient;
        this.repository = repository;
        this.props = props;
    }

    /**
     * Runs one full sync. Overlapping runs are skipped. Any failure aborts the run BEFORE stale
     * records are removed, so a wallet-service outage can never empty the lookup table.
     *
     * @return number of accounts upserted, or -1 if another sync was already running
     */
    public long sync() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Wallet sync already in progress - skipping this trigger");
            return -1;
        }
        // Mongo stores dates with millisecond precision; truncate so the cutoff below compares fairly.
        Instant startedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        try {
            int pageSize = props.sync().pageSize();
            int page = 0;
            int totalPages;
            long upserted = 0;

            do {
                WalletAccountsResponse response = walletClient.fetchPage(page, pageSize);
                List<WalletAccountDto> dtos = response.data().content().stream()
                        .filter(dto -> dto.accountNumber() != null && !dto.accountNumber().isBlank())
                        .toList();
                Map<String, WalletAccount> existing = loadExisting(dtos);
                List<WalletAccount> docs = dtos.stream()
                        .map(dto -> toDocument(dto, existing.get(WalletAccount.keyOf(dto.accountNumber())), startedAt))
                        .toList();
                repository.saveAll(docs);
                upserted += docs.size();

                totalPages = response.data().page() == null ? 1 : response.data().page().totalPages();
                page++;
            } while (page < totalPages);

            long removed = 0;
            if (props.sync().deleteStale()) {
                if (upserted > 0) {
                    removed = repository.deleteByLastSyncedAtBefore(startedAt);
                } else {
                    log.warn("Wallet returned zero accounts - keeping existing records (stale cleanup skipped)");
                }
            }
            log.info("Wallet sync complete: {} accounts upserted, {} stale removed, {} page(s)",
                    upserted, removed, page);
            return upserted;
        } finally {
            running.set(false);
        }
    }

    /** The documents already stored for this page, so null values from wallet can be ignored. */
    private Map<String, WalletAccount> loadExisting(List<WalletAccountDto> dtos) {
        if (!props.sync().preserveExistingOnNull() || dtos.isEmpty()) {
            return Map.of();
        }
        List<String> ids = dtos.stream().map(dto -> WalletAccount.keyOf(dto.accountNumber())).toList();
        return repository.findAllById(ids).stream()
                .collect(Collectors.toMap(WalletAccount::getId, Function.identity(), (a, b) -> a));
    }

    /**
     * Builds the document to save. When {@code old} is present (only loaded if preserve-existing-on-null
     * is on), a null or blank value from wallet-service never replaces a value we already hold.
     */
    private WalletAccount toDocument(WalletAccountDto dto, WalletAccount old, Instant syncedAt) {
        WalletAccount doc = new WalletAccount();
        doc.setId(WalletAccount.keyOf(dto.accountNumber()));
        doc.setAccountNumber(dto.accountNumber().trim());
        doc.setUserId(pick(dto.userId(), old == null ? null : old.getUserId()));
        doc.setFullName(pick(dto.fullName(), old == null ? null : old.getFullName()));
        doc.setFrozen(dto.frozen() != null ? dto.frozen() : old != null && old.isFrozen());

        String mpesa = pick(dto.mpesaPhoneNumber(), old == null ? null : old.getMpesaPhoneNumber());
        doc.setMpesaPhoneNumber(mpesa);
        doc.setMpesaPhoneKey(PhoneNumbers.normalizeMsisdn(mpesa));

        doc.setPochiPhoneNumber(pick(dto.pochiPhoneNumber(), old == null ? null : old.getPochiPhoneNumber()));
        doc.setPaypalEmail(pick(dto.paypalEmail(), old == null ? null : old.getPaypalEmail()));
        doc.setPaypalConnectedEmail(
                pick(dto.paypalConnectedEmail(), old == null ? null : old.getPaypalConnectedEmail()));
        doc.setStripeConnected(dto.stripeConnected() != null ? dto.stripeConnected() : old != null && old.isStripeConnected());
        doc.setStripePayoutsEnabled(
                dto.stripePayoutsEnabled() != null ? dto.stripePayoutsEnabled() : old != null && old.isStripePayoutsEnabled());
        doc.setFlutterwavePaymentMethodNetwork(pick(dto.flutterwavePaymentMethodNetwork(),
                old == null ? null : old.getFlutterwavePaymentMethodNetwork()));
        doc.setFlutterwavePaymentMethodPhone(pick(dto.flutterwavePaymentMethodPhone(),
                old == null ? null : old.getFlutterwavePaymentMethodPhone()));
        doc.setCreatedAt(dto.createdAt() != null ? dto.createdAt() : old == null ? null : old.getCreatedAt());
        doc.setLastSyncedAt(syncedAt);
        return doc;
    }

    /** The incoming value, unless it is null or blank and we already hold one. */
    private static String pick(String incoming, String existing) {
        boolean missing = incoming == null || incoming.isBlank();
        return missing && existing != null && !existing.isBlank() ? existing : incoming;
    }
}