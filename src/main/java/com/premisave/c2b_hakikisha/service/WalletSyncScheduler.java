package com.premisave.c2b_hakikisha.service;

import com.premisave.c2b_hakikisha.config.HakikishaProperties;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Triggers the wallet sync once at startup (if hakikisha.sync.run-on-startup=true) and then every
 * hakikisha.sync.interval-minutes minutes (measured from the end of the previous run).
 * Failures are reported as a single readable log line; the scheduler never stops and lookups keep
 * being served from whatever is already stored in MongoDB.
 */
@Component
@ConditionalOnProperty(prefix = "hakikisha.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WalletSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(WalletSyncScheduler.class);

    private final WalletSyncService syncService;
    private final HakikishaProperties props;
    private final String walletUrl;

    /** True after a failed run, so recovery can be logged once. */
    private volatile boolean lastRunFailed = false;

    public WalletSyncScheduler(WalletSyncService syncService,
                               HakikishaProperties props,
                               @Value("${wallet.service.url}") String walletUrl) {
        this.syncService = syncService;
        this.props = props;
        this.walletUrl = walletUrl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (props.sync().runOnStartup()) {
            run("startup");
        }
    }

    @Scheduled(
            initialDelayString = "PT${hakikisha.sync.interval-minutes:5}M",
            fixedDelayString = "PT${hakikisha.sync.interval-minutes:5}M")
    public void scheduled() {
        run("scheduled");
    }

    private void run(String trigger) {
        int retryMinutes = props.sync().intervalMinutes();
        try {
            log.info("Starting wallet sync ({})", trigger);
            long result = syncService.sync();
            if (result >= 0 && lastRunFailed) {
                log.info("Wallet service is reachable again - sync recovered");
            }
            if (result >= 0) {
                lastRunFailed = false;
            }

        } catch (ResourceAccessException e) {
            lastRunFailed = true;
            String reason = isTimeout(e) ? "the request timed out" : "connection refused or host unreachable";
            log.warn("Wallet service appears to be OFFLINE at {} ({}). Sync skipped - lookups keep using the data "
                    + "already in MongoDB. Next attempt in {} minute(s).", walletUrl, reason, retryMinutes);
            log.trace("Wallet sync connection failure details", e);

        } catch (RestClientResponseException e) {
            lastRunFailed = true;
            int status = e.getStatusCode().value();
            if (status == 401 || status == 403) {
                log.warn("Wallet service rejected the API key (HTTP {}). Check that INTERNAL_API_KEY matches the "
                        + "wallet service's INTERNAL_API_KEY. Next attempt in {} minute(s).", status, retryMinutes);
            } else if (status >= 500) {
                log.warn("Wallet service is reachable but returned a server error (HTTP {}). Sync skipped. "
                        + "Next attempt in {} minute(s).", status, retryMinutes);
            } else {
                log.warn("Wallet service replied with an unexpected HTTP {} for /internal/accounts. Sync skipped. "
                        + "Next attempt in {} minute(s).", status, retryMinutes);
            }
            log.trace("Wallet sync HTTP failure details", e);

        } catch (Exception e) {
            // Unknown failure: keep the full stack trace so it can be diagnosed.
            lastRunFailed = true;
            log.error("Wallet sync ({}) failed unexpectedly: {}", trigger, e.getMessage(), e);
        }
    }

    private static boolean isTimeout(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof HttpTimeoutException || c instanceof SocketTimeoutException) {
                return true;
            }
            if (c.getCause() == c) {
                break;
            }
        }
        return false;
    }
}