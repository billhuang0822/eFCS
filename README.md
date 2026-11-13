# eFCS payment service and mock host

Two independent Maven projects for Java 17+ (also compatible with JDK 21):

- `eFCSPay/`: Spring Boot REST adapter for a frontend. It validates `HEAD.prsCode`, forwards JSON to the host, and returns host JSON.
- `mock/`: local Spring Boot mock host for T101, T102, T112 and T201.

## Requirements

- JDK 17 or 21
- Maven 3.6.3+

Spring Boot 4.1.1 is selected as requested. Verify availability in your Maven repository before building if using a restricted mirror.

## Run locally

Terminal 1:

```sh
cd mock
mvn spring-boot:run
```

Terminal 2:

```sh
cd eFCSPay
mvn spring-boot:run
```

Service listens on `http://localhost:8080`; mock host listens on `http://localhost:8091`.

Send a T101 request to the service:

```sh
curl -X POST http://localhost:8080/api/messages/T101 \
  -H 'Content-Type: application/json' \
  -d '{"HEAD":{"txnChannel":"WEB","prsCode":"T101","txnDataTime":"20261113120000","txnNo":"demo-001","billerId":"BILLER001","bTypeClass":"","icChkCode":"","icChkDesc":""},"BODY":{"queryType":"01","queryData1":"A001","queryData2":"","queryData3":"","queryData4":"","queryData5":"","eBillSn":"","outBankId":"","key":""}}'
```

The service accepts only the supported codes and requires the `prsCode` in the JSON `HEAD` to match the URL code. Configure the upstream host with `EFCS_HOST_BASE_URL` (base URL, default `http://localhost:8091/mock/efcs`). The mock host has a test-only CORS policy allowing localhost frontend development. Add authentication, authorization, TLS, request signing, sensitive-data masking, and stricter validation before production use. Avoid logging full payment payloads.
