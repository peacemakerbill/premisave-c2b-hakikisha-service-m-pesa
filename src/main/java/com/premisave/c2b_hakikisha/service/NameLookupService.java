package com.premisave.c2b_hakikisha.service;

import com.premisave.c2b_hakikisha.config.HakikishaProperties;
import com.premisave.c2b_hakikisha.dto.NameLookupRequest;
import com.premisave.c2b_hakikisha.dto.NameLookupResponse;
import com.premisave.c2b_hakikisha.exception.ApiException;
import com.premisave.c2b_hakikisha.model.WalletAccount;
import com.premisave.c2b_hakikisha.repository.WalletAccountRepository;
import com.premisave.c2b_hakikisha.util.PhoneNumbers;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Name lookup for M-Pesa: the accountNumber in the request is the customer's M-Pesa phone number,
 * matched against the M-Pesa number stored on each wallet account.
 */
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

        // 07XX..., +2547XX... and 2547XX... all resolve to the same 254XXXXXXXXX key.
        String msisdn = PhoneNumbers.normalizeMsisdn(request.accountNumber());
        if (msisdn == null) {
            log.info("Name lookup: account number is not a valid phone number, requestId={}", requestId);
            throw new ApiException(HttpStatus.BAD_REQUEST, requestId, "Invalid account number");
        }

        List<WalletAccount> matches;
        try {
            matches = repository.findByMpesaPhoneKey(msisdn);
        } catch (RuntimeException e) {
            log.error("Name lookup failed for requestId={}: {}", requestId, e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, requestId, "Internal server error");
        }

        // Unknown, frozen, or nameless accounts are all reported as an invalid account number.
        List<WalletAccount> usable = matches.stream()
                .filter(a -> !a.isFrozen() && !isBlank(a.getFullName()))
                .toList();

        if (usable.isEmpty()) {
            log.info("Name lookup: no valid account for requestId={}", requestId);
            throw new ApiException(HttpStatus.BAD_REQUEST, requestId, "Invalid account number");
        }
        if (usable.size() > 1) {
            // Never guess whose name to return when a number is shared between wallets.
            log.warn("Name lookup: {} active wallet accounts share the same M-Pesa number, requestId={} - "
                    + "rejecting as invalid", usable.size(), requestId);
            throw new ApiException(HttpStatus.BAD_REQUEST, requestId, "Invalid account number");
        }

        return new NameLookupResponse(
                requestId,
                request.timestamp(),
                usable.get(0).getFullName(),
                request.accountNumber().trim(),
                props.shortcode());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}