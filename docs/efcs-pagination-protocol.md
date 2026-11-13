# eFCS Payment Message Workflow

This project models the real eFCS message flow for transactions T101 and T102.

## Core protocol rule

For T101 and T102, the transaction is not a single request/response. It is a paginated continuation flow:

1. Client sends request with `BODY.key`.
2. Host replies with a page of data and a next continuation key.
3. If the host returns a non-empty continuation key, the client sends the same transaction again with that exact key.
4. When the host returns `"key": "END"`, the pagination is complete.
5. The client merges `QUERYDETAIL` or `PAYDETAIL` from all pages before presenting the result.

Rules:
- `key` is not optional for a continuation flow.
- `"END"` is the terminal marker.
- Empty string is not treated as terminal in the protocol model used here.

## T101/T102 mock example

- First page: `"key": "MOCK-T101-PAGE-2"` or `"MOCK-T102-PAGE-2"`
- Last page: `"key": "END"`

## Java service behavior

The Java service in `eFCSPay` follows the cursor until the host returns `"END"`. The `QUERYDETAIL`/`PAYDETAIL` entries are cumulatively merged in memory before returning the final payload to the caller.

## Backend mock behavior

The mock server in `mock` simulates two pages and then ends with `"key": "END"`. This lets you test page-following logic without a real host.

## Example request flow

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

The host returns, for example:

```json
{
  "HEAD": { "prsCode": "T101", "icChkCode": "0000", "icChkDesc": "成功" },
  "BODY": {
    "QUERYHEAD": { "totalCount": 2, "totalAmount": 300 },
    "QUERYDETAIL": [ { "queryDetailNo": "1" } ],
    "key": "MOCK-T101-PAGE-2"
  }
}
```

Then the next request uses the returned key:

```json
{
  "HEAD": { "prsCode": "T101" },
  "BODY": {
    "queryType": "01",
    "queryData1": "A001",
    "key": "MOCK-T101-PAGE-2"
  }
}
```

And the final page returns:

```json
{
  "HEAD": { "prsCode": "T101", "icChkCode": "0000", "icChkDesc": "成功" },
  "BODY": {
    "QUERYHEAD": { "totalCount": 2, "totalAmount": 300 },
    "QUERYDETAIL": [ { "queryDetailNo": "2" } ],
    "key": "END"
  }
}
```
