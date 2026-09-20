package com.premisave.c2b_hakikisha.client;

import com.premisave.c2b_hakikisha.config.HakikishaProperties;
import com.premisave.c2b_hakikisha.dto.WalletAccountsResponse;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Calls wallet-service GET /internal/accounts using the shared X-API-Key. */
@Component
public class WalletServiceClient {

    private final RestClient restClient;

    public WalletServiceClient(
            @Value("${wallet.service.url}") String baseUrl,
            @Value("${internal.api-key}") String apiKey,
            HakikishaProperties props) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(props.sync().connectTimeoutSeconds()))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(props.sync().readTimeoutSeconds()));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.replaceAll("/+$", ""))
                .defaultHeader("X-API-Key", apiKey)
                .requestFactory(factory)
                .build();
    }

    public WalletAccountsResponse fetchPage(int page, int size) {
        WalletAccountsResponse response = restClient.get()
                .uri(uri -> uri.path("/internal/accounts")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .body(WalletAccountsResponse.class);

        if (response == null || !response.success() || response.data() == null || response.data().content() == null) {
            throw new IllegalStateException("Unexpected response from wallet-service for page " + page);
        }
        return response;
    }
}