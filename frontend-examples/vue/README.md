# Vue pagination example

`src/useEfcsPagedQuery.ts` automatically fetches T101/T102 pages sequentially from the browser. Each request to `POST /api/messages/{prsCode}` makes the service proxy exactly one host page. The composable carries forward `BODY.key`, aggregates detail arrays, and stops at `END`; the UI may then reveal the collected rows locally.

A maximum page count, cursor loop detection, loading/error state, and request cancellation protect the client flow. An individual upstream call may still time out. Do not blindly retry T102 after an ambiguous timeout; follow the host's idempotency and status-query procedures. Authenticate and authorize every page request on the server.
