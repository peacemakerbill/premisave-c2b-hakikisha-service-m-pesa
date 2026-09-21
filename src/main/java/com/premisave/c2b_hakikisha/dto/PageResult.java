package com.premisave.c2b_hakikisha.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** Page of results shaped like the wallet service's: {content, page:{size, number, totalElements, totalPages}}. */
public record PageResult<T>(List<T> content, PageInfo page) {

    public record PageInfo(int size, int number, long totalElements, int totalPages) {
    }

    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(
                page.getContent(),
                new PageInfo(page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()));
    }
}