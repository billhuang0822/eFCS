# Browser-driven continuation pagination

For T101/T102, the browser owns the continuation key and requests one page at a time. Each `POST /api/messages/{prsCode}` call proxies exactly one host request. The server does not loop through cursors or aggregate pages, avoiding a long-running browser-to-server request.

1. Browser sends the first request with `BODY.key: ""`.
2. Server proxies that single request and returns its response, including the host's next key.
3. On **下一頁**, browser resends the same transaction data with `BODY.key` set to the last response's `BODY.key`.
4. Stop when the returned key is exactly `END`.

Keep the endpoint authenticated and authorize every page request. Validate that the cursor belongs to the same user and transaction where the host protocol permits; never trust client-provided transaction fields or use blind retries for payment operations. Avoid logging sensitive body data.
