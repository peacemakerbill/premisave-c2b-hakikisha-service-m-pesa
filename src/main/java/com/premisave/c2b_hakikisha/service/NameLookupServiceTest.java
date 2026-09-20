package com.premisave.c2b_hakikisha.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.premisave.c2b_hakikisha.config.HakikishaProperties;
import com.premisave.c2b_hakikisha.dto.NameLookupRequest;
import com.premisave.c2b_hakikisha.dto.NameLookupResponse;
import com.premisave.c2b_hakikisha.exception.ApiException;
import com.premisave.c2b_hakikisha.model.WalletAccount;
import com.premisave.c2b_hakikisha.repository.WalletAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class NameLookupServiceTest {

    private WalletAccountRepository repository;
    private NameLookupService service;

    @BeforeEach
    void setUp() {
        repository = mock(WalletAccountRepository.class);
        HakikishaProperties props = new HakikishaProperties(
                "600992",
                new HakikishaProperties.Auth("user", "pass", 3599),
                new HakikishaProperties.Sync(true, 5, true, 100, true, 10, 30));
        service = new NameLookupService(repository, props);
    }

    private static WalletAccount account(String number, String name, boolean frozen) {
        WalletAccount a = new WalletAccount();
        a.setId(WalletAccount.keyOf(number));
        a.setAccountNumber(number);
        a.setFullName(name);
        a.setFrozen(frozen);
        return a;
    }

    @Test
    void returnsNameForActiveAccount_caseInsensitive() {
        when(repository.findById("bill@example.com"))
                .thenReturn(Optional.of(account("bill@example.com", "Bill Graham Peacemaker", false)));

        NameLookupResponse res = service.lookup(
                new NameLookupRequest("req-1", "1728897681", "Bill@Example.com", "600992"));

        assertThat(res.accountName()).isEqualTo("Bill Graham Peacemaker");
        assertThat(res.requestId()).isEqualTo("req-1");
        assertThat(res.timestamp()).isEqualTo("1728897681");
        assertThat(res.shortcode()).isEqualTo("600992");
    }

    @Test
    void frozenAccountIsReportedAsInvalidAccountNumber() {
        when(repository.findById("frozen@example.com"))
                .thenReturn(Optional.of(account("frozen@example.com", "Frozen User", true)));

        assertThatThrownBy(() -> service.lookup(
                new NameLookupRequest("req-2", "1", "frozen@example.com", "600992")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getMessage()).isEqualTo("Invalid account number");
                    assertThat(e.getRequestId()).isEqualTo("req-2");
                });
    }

    @Test
    void unknownAccountIsInvalid() {
        when(repository.findById("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lookup(
                new NameLookupRequest("req-3", "1", "nobody@example.com", "600992")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void wrongShortcodeIsRejected() {
        assertThatThrownBy(() -> service.lookup(
                new NameLookupRequest("req-4", "1", "a@b.com", "111111")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getMessage()).isEqualTo("Invalid shortcode");
                });
    }

    @Test
    void missingFieldsReturn422() {
        assertThatThrownBy(() -> service.lookup(new NameLookupRequest("req-5", "1", null, "600992")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT));
        assertThatThrownBy(() -> service.lookup(new NameLookupRequest("req-6", "1", "a@b.com", " ")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT));
    }

    @Test
    void databaseFailureReturns500() {
        when(repository.findById("a@b.com")).thenThrow(new IllegalStateException("mongo down"));

        assertThatThrownBy(() -> service.lookup(new NameLookupRequest("req-7", "1", "a@b.com", "600992")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(e.getMessage()).isEqualTo("Internal server error");
                });
    }
}