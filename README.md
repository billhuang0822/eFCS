# eFCS payment service and mock host

Two independent Spring Boot Maven projects support Java 17+ (including JDK 21):

- `eFCSPay/`: frontend-facing REST adapter. It validates `HEAD.prsCode` and proxies exactly one host request per HTTP request.
- `mock/`: local mock host for T101, T102, T112 and T201.
- `eFCSUtility/`: standalone Jackson 3 Spring Boot utility/client scaffold.
- `frontend-examples/`: React, Vue and plain HTML examples.

## T101/T102 continuation paging

The browser drives continuation paging to avoid holding one browser-to-server request open while the server retrieves every host page. For each page the browser sends `POST /api/messages/{prsCode}` with the same transaction data and the current `BODY.key`. `eFCSPay` makes one host call and returns that page and its next key. The browser then automatically sends the next request, sequentially, until the host returns `BODY.key: "END"`; the UI can aggregate `QUERYDETAIL` or `PAYDETAIL` as pages arrive.

This means one browser-side async operation may contain multiple short HTTP requests; it does not mean the backend loops through all host pages in one request. Cursor calls must be sequential because each next cursor is only known from the preceding response. Empty/repeated cursors and excessive page counts should stop the flow. See [browser-driven pagination](docs/browser-driven-pagination.md) and [the eFCS pagination protocol](docs/efcs-pagination-protocol.md).

T112 and T201 are single request/response transactions in the supplied examples. Do not blindly retry T102 after an ambiguous timeout; follow the host's idempotency or transaction-status procedure.

## Run locally

Requirements: JDK 17+ and Maven 3.6.3+.

Start the mock host:

```sh
cd mock
mvn spring-boot:run
```

In another terminal, start eFCSPay:

```sh
cd eFCSPay
mvn spring-boot:run
```

The service listens on `http://localhost:8080`; mock host listens on `http://localhost:8091`. Configure the host base URL using `EFCS_HOST_BASE_URL` (default `http://localhost:8091/mock/efcs`). The mock host has a test-only CORS policy for local frontend development.

Run checks:

```sh
(cd eFCSPay && mvn test)
(cd mock && mvn test)
(cd eFCSUtility && mvn test)
```

Before production use, add authentication and authorization on every page request, validate that the cursor and transaction belong to the authenticated user, use TLS, apply protocol-required request signing, mask sensitive data, and avoid logging payment payloads. Jackson 3 reduces exposure to Jackson 2 vulnerabilities only when the resolved dependency tree is actually Jackson 3 and kept patched; run dependency/CVE scanning on the built artifact.
