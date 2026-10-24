package com.premisave.c2b_hakikisha.service;

import com.premisave.c2b_hakikisha.config.HakikishaProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Safety net for name lookups. When a lookup finds no usable account in MongoDB, this runs one wallet
 * sync immediately instead of waiting for the next scheduled run, so a wallet-service that was offline
 * during earlier syncs does not leave customers unresolved once it is back.
 *
 * <p>It never makes a lookup wait long: the caller waits at most hakikisha.sync.fallback.wait-seconds,
 * the sync carries on in the background, and a new fallback is not started again until
 * hakikisha.sync.fallback.cooldown-seconds have passed. Concurrent lookups share one running sync.
 * The scheduled sync is unchanged.
 */
@Service
public class WalletLookupFallback implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(WalletLookupFallback.class);

    private final WalletSyncService syncService;
    private final HakikishaProperties props;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "wallet-lookup-fallback");
        thread.setDaemon(true);
        return thread;
    });

    /** Both guarded by this. */
    private CompletableFuture<Void> current;
    private Instant lastStart = Instant.EPOCH;

    public WalletLookupFallback(WalletSyncService syncService, HakikishaProperties props) {
        this.syncService = syncService;
        this.props = props;
    }

    /**
     * Runs (or joins) a wallet sync and waits for it, up to the configured limit.
     *
     * @return true if a sync finished in time, so the caller should look in MongoDB again;
     *         false if the fallback is off, is cooling down, or did not finish in time
     */
    public boolean refresh() {
        HakikishaProperties.Sync sync = props.sync();
        if (!sync.enabled() || !sync.fallback().enabled()) {
            return false;
        }

        CompletableFuture<Void> attempt;
        synchronized (this) {
            if (current != null && !current.isDone()) {
                attempt = current;
            } else if (Duration.between(lastStart, Instant.now()).getSeconds() < sync.fallback().cooldownSeconds()) {
                return false;
            } else {
                lastStart = Instant.now();
                current = CompletableFuture.runAsync(this::runSync, executor);
                attempt = current;
            }
        }

        try {
            attempt.get(sync.fallback().waitSeconds(), TimeUnit.SECONDS);
            return true;
        } catch (TimeoutException e) {
            log.warn("Fallback sync did not finish within {}s. Answering from stored data; the sync keeps "
                    + "running and later lookups will use its result.", sync.fallback().waitSeconds());
            return false;
        } catch (ExecutionException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void runSync() {
        try {
            log.info("Name lookup found no usable account, refreshing from wallet-service now");
            syncService.sync();
        } catch (ResourceAccessException e) {
            log.warn("Fallback sync skipped: wallet-service is unreachable ({})", e.getMessage());
        } catch (RestClientResponseException e) {
            log.warn("Fallback sync skipped: wallet-service replied HTTP {}", e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("Fallback sync failed: {}", e.getMessage());
            log.debug("Fallback sync failure details", e);
        }
    }

    @Override
    public void destroy() {
        executor.shutdownNow();
    }
}