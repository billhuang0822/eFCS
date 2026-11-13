# eFCS T101/T102 pagination protocol and implementation

## Protocol rule

T101 and T102 are cursor-based continuation flows. The request's `BODY.key` is the current cursor. Each host response includes one page of `QUERYDETAIL` (T101) or `PAYDETAIL` (T102), plus `BODY.key` containing either the next cursor or the terminal marker `END`.

- Start with the protocol-defined initial cursor (the mock uses an empty string).
- For a non-terminal response, send the same transaction data with the returned key in `BODY.key`.
- `END`—not an empty string—marks completion.
- Preserve the transaction identity/header across continuation calls unless the host protocol explicitly specifies otherwise.

## Browser-driven architecture

The browser automatically issues these requests sequentially so one web request does not stay open while the server fetches every host page:

1. Browser sends initial request to `POST /api/messages/T101` or `/api/messages/T102`.
2. The service validates the URL and `HEAD.prsCode`, makes exactly one host request, and returns that page and its cursor.
3. Browser appends the page's detail entries and automatically sends the next request with the returned `BODY.key`.
4. Browser stops at `BODY.key === "END"` and presents the aggregated detail rows.

The backend must not loop over the cursor in one request. Each individual browser-to-service call is short relative to fetching all pages, though any single host call can still take long enough to time out. The browser should not issue pages in parallel because it cannot know the next cursor before the prior response.

The mock uses `MOCK-T101-PAGE-2` / `MOCK-T102-PAGE-2` as the first page's next cursor and returns `END` on the second page.

## Sample request and continuation

Initial T101 request:

```json
{
  "HEAD": {
    "txnChannel": "WEB",
    "prsCode": "T101",
    "txnDataTime": "20261113120000",
    "txnNo": "demo-001",
    "billerId": "BILLER001",
    "bTypeClass": "",
    "icChkCode": "",
    "icChkDesc": ""
  },
  "BODY": {
    "queryType": "01",
    "queryData1": "A001",
    "queryData2": "",
    "queryData3": "",
    "queryData4": "",
    "queryData5": "",
    "eBillSn": "",
    "outBankId": "",
    "key": ""
  }
}
```

If the first response contains `BODY.key: "MOCK-T101-PAGE-2"`, the browser sends the same request data again with that key. The response's `BODY.key: "END"` signals the final page. The client combines `QUERYDETAIL` across pages. For T102, it combines `PAYDETAIL` in the same manner.

## Implementation references

- Backend one-page proxy: `eFCSPay/src/main/java/com/efcs/pay/service/EfcsMessageService.java`
- React Fetch and Axios hooks: `frontend-examples/react/src/useEfcsPagedQuery.ts` and `useEfcsPagedQueryAxios.ts`
- Vue composable: `frontend-examples/vue/src/useEfcsPagedQuery.ts`
- Standalone demos: `frontend-examples/html/fetch.html` and `axios.html`

## Safety and operations

Authorize every page call and bind the cursor to the authenticated user/transaction when possible. Treat client-supplied request bodies and cursors as untrusted input. Cap page count and reject empty/repeated cursors. Do not automatically replay T102 after an ambiguous timeout: use the host's idempotency and transaction-status procedures. Avoid logging sensitive payload fields.
