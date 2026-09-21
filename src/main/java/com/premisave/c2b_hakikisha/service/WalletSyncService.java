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
import java.util.concurrent.atomic.AtomicBoolean;
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
                List<WalletAccount> docs = response.data().content().stream()
                        .filter(dto -> dto.accountNumber() != null && !dto.accountNumber().isBlank())
                        .map(dto -> toDocument(dto, startedAt))
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

    private WalletAccount toDocument(WalletAccountDto dto, Instant syncedAt) {
        WalletAccount doc = new WalletAccount();
        doc.setId(WalletAccount.keyOf(dto.accountNumber()));
        doc.setAccountNumber(dto.accountNumber().trim());
        doc.setUserId(dto.userId());
        doc.setFullName(dto.fullName());
        doc.setFrozen(Boolean.TRUE.equals(dto.frozen()));
        doc.setMpesaPhoneNumber(dto.mpesaPhoneNumber());
        doc.setMpesaPhoneKey(PhoneNumbers.normalizeMsisdn(dto.mpesaPhoneNumber()));
        doc.setPochiPhoneNumber(dto.pochiPhoneNumber());
        doc.setPaypalEmail(dto.paypalEmail());
        doc.setPaypalConnectedEmail(dto.paypalConnectedEmail());
        doc.setStripeConnected(Boolean.TRUE.equals(dto.stripeConnected()));
        doc.setStripePayoutsEnabled(Boolean.TRUE.equals(dto.stripePayoutsEnabled()));
        doc.setFlutterwavePaymentMethodNetwork(dto.flutterwavePaymentMethodNetwork());
        doc.setFlutterwavePaymentMethodPhone(dto.flutterwavePaymentMethodPhone());
        doc.setCreatedAt(dto.createdAt());
        doc.setLastSyncedAt(syncedAt);
        return doc;
    }
}