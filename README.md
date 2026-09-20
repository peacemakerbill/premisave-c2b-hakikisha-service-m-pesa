# premisave-c2b-hakikisha-service-m-pesa

Safaricom **C2B Hakikisha** (name lookup) provider for Premisave. Spring Boot 4.2.0-SNAPSHOT, Java 21, MongoDB.

## How it works
1. **Sync job** – on startup and every `HAKIKISHA_SYNC_INTERVAL_MINUTES` (default 5) it pages through
   `GET {WALLET_SERVICE_URL}/internal/accounts` (header `X-API-Key`) and upserts every account into the
   `wallet_accounts` collection. Accounts that disappear from wallet are removed after a fully successful run.
2. **Token endpoint** – `POST /oauth2/v1/generate?grant_type=client_credentials` with Basic Auth
   (`HAKIKISHA_USERNAME` / `HAKIKISHA_PASSWORD`) -> `{"access_token","expires_in":"3599"}`.
3. **Name lookup** – `POST /api/v1/c2b/hakikisha/name-lookup` with `Authorization: Bearer <access_token>`.

| Situation | HTTP | Body |
|---|---|---|
| Found | 200 | `requestId, timestamp, accountName, accountNumber, shortcode` |
| Unknown / **frozen** account | 400 | `{"requestId","errorMessage":"Invalid account number"}` |
| Wrong shortcode | 400 | `{"requestId","errorMessage":"Invalid shortcode"}` |
| Missing `accountNumber`/`shortcode`, or bad JSON | 422 | `{"requestId","errorMessage":"Missing required fields"}` |
| Server/database failure | 500 | `{"requestId","errorMessage":"Internal server error"}` |
| Bad client credentials / bad or expired token | 401 | `{"errorCode":"401","errorMessage":"..."}` |

## Run
```bash
# 1. edit .env (INTERNAL_API_KEY must equal wallet-service's; change the default HAKIKISHA_PASSWORD)
# 2. make sure MongoDB and wallet-service are up
./mvnw spring-boot:run        # or: mvn spring-boot:run
```
`.env` is loaded through `spring.config.import`; real environment variables (Docker/K8s) also work and take precedence.

## Try it
```bash
TOKEN=$(curl -s -X POST 'http://localhost:8090/oauth2/v1/generate?grant_type=client_credentials' \
  -u "$HAKIKISHA_USERNAME:$HAKIKISHA_PASSWORD" | sed -E 's/.*"access_token":"([^"]+)".*/\1/')

curl -s -X POST http://localhost:8090/api/v1/c2b/hakikisha/name-lookup \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"requestId":"dcd1c2ab-7a26-4170-939d-9dc2e879b0e5","timestamp":"1728897681","accountNumber":"grahambill011@gmail.com","shortcode":"600992"}'
```

## Tuning the sync (application.yml / .env)
`HAKIKISHA_SYNC_ENABLED`, `HAKIKISHA_SYNC_INTERVAL_MINUTES`, `HAKIKISHA_SYNC_RUN_ON_STARTUP`,
`HAKIKISHA_SYNC_PAGE_SIZE`, `HAKIKISHA_SYNC_DELETE_STALE`, and the connect/read timeouts.
