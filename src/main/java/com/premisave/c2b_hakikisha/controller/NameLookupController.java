package com.premisave.c2b_hakikisha.controller;

import com.premisave.c2b_hakikisha.dto.NameLookupRequest;
import com.premisave.c2b_hakikisha.dto.NameLookupResponse;
import com.premisave.c2b_hakikisha.service.NameLookupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Step 2: account-name retrieval. Protected by the Bearer interceptor (see AppConfig). */
@RestController
@RequestMapping("/api/v1/c2b/hakikisha")
public class NameLookupController {

    private final NameLookupService lookupService;

    public NameLookupController(NameLookupService lookupService) {
        this.lookupService = lookupService;
    }

    @PostMapping("/name-lookup")
    public ResponseEntity<NameLookupResponse> lookup(@RequestBody NameLookupRequest request) {
        return ResponseEntity.ok(lookupService.lookup(request));
    }
}