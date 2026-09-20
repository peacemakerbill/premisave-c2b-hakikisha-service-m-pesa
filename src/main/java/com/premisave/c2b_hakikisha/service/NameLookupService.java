package com.premisave.c2b_hakikisha.service;

import com.premisave.c2b_hakikisha.config.HakikishaProperties;
import com.premisave.c2b_hakikisha.dto.NameLookupRequest;
import com.premisave.c2b_hakikisha.dto.NameLookupResponse;
import com.premisave.c2b_hakikisha.exception.ApiException;
import com.premisave.c2b_hakikisha.model.WalletAccount;
import com.premisave.c2b_hakikisha.repository.WalletAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class NameLookupService {

    private static final Logger log = LoggerFactory.getLogger(NameLookupService.class);

    private final WalletAccountRepository repository;
    private final HakikishaProperties props;

    public NameLookupService(WalletAccountRepository repository, HakikishaProperties props) {
        this.repository = repository;
        this.props = props;
    }

    public NameLookupResponse lookup(NameLookupRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, null, "Missing required fields");
        }
        String requestId = request.requestId();

        if (isBlank(request.accountNumber()) || isBlank(request.shortcode())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, requestId, "Missing required fields");
        }
        if (!props.shortcode().equals(request.shortcode().trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, requestId, "Invalid shortcode");
        }

        WalletAccount account;
        try {
            account = repository.findById(WalletAccount.keyOf(request.accountNumber())).orElse(null);
        } catch (RuntimeException e) {
            log.error("Name lookup failed for requestId={}: {}", requestId, e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, requestId, "Internal server error");
        }

        // Unknown, frozen, or nameless accounts are all reported as an invalid account number.
        if (account == null || account.isFrozen() || isBlank(account.getFullName())) {
            log.info("Name lookup: no valid account for requestId={}", requestId);
            throw new ApiException(HttpStatus.BAD_REQUEST, requestId, "Invalid account number");
        }

        return new NameLookupResponse(
                requestId,
                request.timestamp(),
                account.getFullName(),
                request.accountNumber().trim(),
                props.shortcode());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}