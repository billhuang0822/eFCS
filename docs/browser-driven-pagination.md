# Browser-driven continuation pagination

T101/T102 host paging must be initiated by the browser to avoid keeping one browser-to-server request open for the entire query. The server proxies exactly one host page for each `POST /api/messages/{prsCode}` request; the browser reads the returned `BODY.key` and automatically issues the next request until the key is exactly `END`.

The React Fetch hook, React Axios hook, Vue composable, and standalone HTML Fetch/Axios demos now use sequential cursor requests and aggregate the returned detail arrays in the browser. The form example follows the same design for T101/T102. The UI can still show a local “display more” control after aggregation; this does not contact the host.

Safety behavior in the examples:
- Requests are sequential, not parallel, because each cursor comes from the previous response.
- Repeated/empty keys and an excessive page count stop the flow.
- React/Vue requests can be canceled when the view changes or unmounts.
- Do not blindly retry T102 payment operations after ambiguous timeout; use the host's idempotency/status-query protocol.

Every page must still authenticate/authorize the user and validate that the cursor/transaction data belong to the same transaction. Do not rely on browser state as an authorization control. One host call per server request reduces the long request risk but each individual host page call can still time out and needs sensible transport timeouts.
