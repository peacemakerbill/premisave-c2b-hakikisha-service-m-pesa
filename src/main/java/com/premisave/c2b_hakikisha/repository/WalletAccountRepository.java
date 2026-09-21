package com.premisave.c2b_hakikisha.repository;

import com.premisave.c2b_hakikisha.model.WalletAccount;
import java.time.Instant;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WalletAccountRepository extends MongoRepository<WalletAccount, String> {

    /** All accounts whose normalised M-Pesa number equals {@code mpesaPhoneKey} (254XXXXXXXXX). */
    List<WalletAccount> findByMpesaPhoneKey(String mpesaPhoneKey);

    /** Removes accounts not touched by the sync run that started at {@code cutoff}. */
    long deleteByLastSyncedAtBefore(Instant cutoff);
}