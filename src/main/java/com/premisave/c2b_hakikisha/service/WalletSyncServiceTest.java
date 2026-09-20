package com.premisave.c2b_hakikisha.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.premisave.c2b_hakikisha.client.WalletServiceClient;
import com.premisave.c2b_hakikisha.config.HakikishaProperties;
import com.premisave.c2b_hakikisha.dto.WalletAccountDto;
import com.premisave.c2b_hakikisha.dto.WalletAccountsResponse;
import com.premisave.c2b_hakikisha.repository.WalletAccountRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WalletSyncServiceTest {

    private WalletServiceClient client;
    private WalletAccountRepository repository;
    private WalletSyncService service;

    @BeforeEach
    void setUp() {
        client = mock(WalletServiceClient.class);
        repository = mock(WalletAccountRepository.class);
        HakikishaProperties props = new HakikishaProperties(
                "600992",
                new HakikishaProperties.Auth("user", "pass", 3599),
                new HakikishaProperties.Sync(true, 5, true, 2, true, 10, 30));
        service = new WalletSyncService(client, repository, props);
    }

    private static WalletAccountDto dto(String number, String name, boolean frozen) {
        return new WalletAccountDto("uid", number, name, frozen, null, null, null, null,
                false, false, null, null, null);
    }

    private static WalletAccountsResponse page(int number, int totalPages, WalletAccountDto... items) {
        return new WalletAccountsResponse(true, "ok",
                new WalletAccountsResponse.Data(List.of(items),
                        new WalletAccountsResponse.PageInfo(2, number, 3, totalPages)),
                "now");
    }

    @Test
    void readsEveryPageAndRemovesStaleRecords() {
        when(client.fetchPage(0, 2)).thenReturn(page(0, 2, dto("a@x.com", "A", false), dto("b@x.com", "B", true)));
        when(client.fetchPage(1, 2)).thenReturn(page(1, 2, dto("c@x.com", "C", false)));

        long upserted = service.sync();

        assertThat(upserted).isEqualTo(3);
        verify(repository, org.mockito.Mockito.times(2)).saveAll(anyList());
        verify(repository).deleteByLastSyncedAtBefore(any(Instant.class));
    }

    @Test
    void failureMidwayNeverDeletesAnything() {
        when(client.fetchPage(0, 2)).thenReturn(page(0, 2, dto("a@x.com", "A", false)));
        when(client.fetchPage(1, 2)).thenThrow(new IllegalStateException("wallet down"));

        assertThatThrownBy(() -> service.sync()).isInstanceOf(IllegalStateException.class);

        verify(repository, never()).deleteByLastSyncedAtBefore(any(Instant.class));
    }

    @Test
    void emptyWalletResponseKeepsExistingRecords() {
        when(client.fetchPage(anyInt(), anyInt())).thenReturn(page(0, 0));

        assertThat(service.sync()).isZero();

        verify(repository, never()).deleteByLastSyncedAtBefore(any(Instant.class));
    }
}