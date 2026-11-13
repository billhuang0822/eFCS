# eFCS payment service and mock host

Two independent Maven projects for Java 17+ (also compatible with JDK 21):

- `eFCSPay/`: frontend-facing Spring Boot service, using transaction-specific DTOs to validate and forward messages.
- `mock/`: standalone mock host with typed responses for T101, T102, T112 and T201.

## Requirements

- JDK 17 or 21
- Maven 3.6.3+
- Spring Boot 4.1.1

## Run locally

Terminal 1:

```sh
cd mock && mvn spring-boot:run
```

Terminal 2:

```sh
cd eFCSPay && mvn spring-boot:run
```

Service listens on `http://localhost:8080`; mock host listens on `http://localhost:8091`.

The service API is `POST /api/messages/{prsCode}`. The message JSON uses the exact casing and structure in `Doc/T101Request.json`, `Doc/T102Request.json`, `Doc/T112Request.json`, and `Doc/T201Request.json`. The host base URL can be set with `EFCS_HOST_BASE_URL` (default `http://localhost:8091/mock/efcs`). Mock response structures follow the corresponding response JSON files in `Doc/`.

Example T101 request:

```sh
curl -X POST http://localhost:8080/api/messages/T101 \
  -H 'Content-Type: application/json' \
  -d '{"HEAD":{"txnChannel":"WEB","prsCode":"T101","txnDataTime":"20261113120000","txnNo":"demo-001","billerId":"BILLER001","bTypeClass":"","icChkCode":"","icChkDesc":""},"BODY":{"queryType":"01","queryData1":"A001","queryData2":"","queryData3":"","queryData4":"","queryData5":"","eBillSn":"","outBankId":"","key":""}}'
```

DTO classes reside in `com.efcs.pay.dto.MessageDtos`; response T112 intentionally has HEAD only, matching the provided JSON sample. To compile each independent Maven project, run `mvn test` from its directory. Before production, add access control, TLS, request signing, stricter per-field validation, transaction idempotency, and sensitive-data masking. Do not log full payment payloads.
