# Browser-driven continuation pagination

## Why the browser drives T101/T102 paging

A server-side loop that calls the host until `END` keeps one browser-to-web-server request open for the entire query. If multiple host pages are slow, that request may exceed browser, gateway, load balancer, or web-server timeouts. Instead, the browser automatically performs a sequence of short requests. Every request still goes through the application server; the browser never calls the EFCS host directly.

## Request lifecycle

For each T101/T102 page, the browser sends `POST /api/messages/{prsCode}`. The server validates the transaction code and proxies exactly one request to the host, then returns that host page unchanged. The client preserves the original transaction data and replaces only `BODY.key` with the cursor returned by the prior page.

```text
Browser                         eFCSPay                         Host
   | POST BODY.key=""              |                              |
   |------------------------------>| POST one page                 |
   |                               |----------------------------->|
   |                               |<-----------------------------| page + key=K1
   |<------------------------------|                              |
   | POST BODY.key="K1"            |                              |
   |------------------------------>| POST one page                 |
   |                               |----------------------------->|
   |                               |<-----------------------------| page + key=END
   |<------------------------------|                              |
```

The browser should await each page before requesting the next; parallel requests cannot safely advance a cursor that has not yet been received. Continue until the returned key is exactly `END`. Merge `QUERYDETAIL` for T101 or `PAYDETAIL` for T102 in the browser as each page arrives. Any local “show more” control after that is only UI rendering and does not contact the host.

## Example pseudocode

```js
let cursor = request.BODY.key || "";
const seen = new Set();
const rows = [];

for (let pageNo = 1; pageNo <= MAX_PAGES; pageNo++) {
  const pageRequest = structuredClone(request);
  pageRequest.BODY.key = cursor;
  const page = await postToEfcspay(pageRequest); // one host page per HTTP request
  rows.push(...page.BODY[detailKey]);

  const next = page.BODY.key;
  if (next === "END") break;
  if (!next || next === cursor || seen.has(next)) throw new Error("Invalid/repeated cursor");
  seen.add(next);
  cursor = next;
}
```

The actual examples include page limits, response validation, cancellation (where supported), progress/error state, and detail aggregation. See React `useEfcsPagedQuery` / `useEfcsPagedQueryAxios`, Vue `useEfcsPagedQuery`, and the standalone HTML Fetch/Axios demos.

## Important constraints

- This reduces the duration of each browser-to-server request; it does not guarantee a single host-page call cannot time out. Configure a reasonable upstream timeout and handle host errors.
- Require authentication/authorization on every page request. Validate that the cursor and transaction data are associated with the same user and transaction. Browser state is not an authorization boundary.
- Consider storing cursor/transaction state server-side and issuing an opaque, user-bound continuation token if host cursors are sensitive or can be tampered with.
- Do not blindly retry T102 payment requests after ambiguous timeouts. Use the host's idempotency key and status-query rules to avoid duplicate payment effects.
- Impose a maximum page count and detect repeated or empty cursors to avoid accidental loops.
- Do not log full request/response bodies containing account, card, mobile, or other sensitive values.
