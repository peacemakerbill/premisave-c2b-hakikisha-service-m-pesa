package com.premisave.c2b_hakikisha.repository;

import com.premisave.c2b_hakikisha.model.WalletAccount;
import java.time.Instant;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WalletAccountRepository extends MongoRepository<WalletAccount, String> {

    /** Removes accounts not touched by the sync run that started at {@code cutoff}. */
    long deleteByLastSyncedAtBefore(Instant cutoff);
}