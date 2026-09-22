package com.premisave.c2b_hakikisha.controller;

import com.premisave.c2b_hakikisha.dto.ApiEnvelope;
import com.premisave.c2b_hakikisha.dto.PageResult;
import com.premisave.c2b_hakikisha.dto.SavedAccountDto;
import com.premisave.c2b_hakikisha.service.AccountListService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Lists the accounts saved by the wallet sync. Protected by the Bearer interceptor (see AppConfig). */
@RestController
@RequestMapping("/internal/accounts")
public class InternalAccountsController {

    private final AccountListService accountListService;

    public InternalAccountsController(AccountListService accountListService) {
        this.accountListService = accountListService;
    }

    @GetMapping
    public ApiEnvelope<PageResult<SavedAccountDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean frozen) {

        return ApiEnvelope.ok("Saved accounts retrieved", accountListService.list(page, size, frozen));
    }
}