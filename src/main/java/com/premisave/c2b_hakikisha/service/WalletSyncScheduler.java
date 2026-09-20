package com.premisave.c2b_hakikisha.service;

import com.premisave.c2b_hakikisha.config.HakikishaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers the wallet sync once at startup (if hakikisha.sync.run-on-startup=true) and then every
 * hakikisha.sync.interval-minutes minutes (measured from the end of the previous run).
 */
@Component
@ConditionalOnProperty(prefix = "hakikisha.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WalletSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(WalletSyncScheduler.class);

    private final WalletSyncService syncService;
    private final HakikishaProperties props;

    public WalletSyncScheduler(WalletSyncService syncService, HakikishaProperties props) {
        this.syncService = syncService;
        this.props = props;
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
        try {
            log.info("Starting wallet sync ({})", trigger);
            syncService.sync();
        } catch (Exception e) {
            // Never let a failed sync kill the scheduler; existing data keeps serving lookups.
            log.error("Wallet sync ({}) failed: {}", trigger, e.getMessage(), e);
        }
    }
}