package com.premisave.c2b_hakikisha.service;

import com.premisave.c2b_hakikisha.dto.PageResult;
import com.premisave.c2b_hakikisha.dto.SavedAccountDto;
import com.premisave.c2b_hakikisha.model.WalletAccount;
import com.premisave.c2b_hakikisha.repository.WalletAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Read-only listing of the accounts saved by the wallet sync. */
@Service
public class AccountListService {

    static final int MAX_PAGE_SIZE = 200;

    private final WalletAccountRepository repository;

    public AccountListService(WalletAccountRepository repository) {
        this.repository = repository;
    }

    /**
     * @param page   zero-based page number (negative values are treated as 0)
     * @param size   page size, clamped to 1..200
     * @param frozen optional filter: true = only frozen accounts, false = only active ones, null = all
     */
    public PageResult<SavedAccountDto> list(int page, int size, Boolean frozen) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.ASC, "id"));

        Page<WalletAccount> result = frozen == null
                ? repository.findAll(pageable)
                : repository.findByFrozen(frozen, pageable);

        return PageResult.of(result.map(SavedAccountDto::from));
    }
}