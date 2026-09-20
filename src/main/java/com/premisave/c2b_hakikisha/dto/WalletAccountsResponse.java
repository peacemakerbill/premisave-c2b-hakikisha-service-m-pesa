package com.premisave.c2b_hakikisha.dto;

import java.util.List;

/** Envelope returned by wallet-service: {success, message, data:{content:[...], page:{...}}, timestamp}. */
public record WalletAccountsResponse(
        boolean success,
        String message,
        Data data,
        String timestamp) {

    public record Data(List<WalletAccountDto> content, PageInfo page) {
    }

    public record PageInfo(int size, int number, long totalElements, int totalPages) {
    }
}