# HTML form and iframe/popup demos

- `form.html` is an asynchronous form for T101, T102, T112 and T201. T101/T102 automatically fetch host pages sequentially from the browser; each request to eFCSPay proxies exactly one host page. The form aggregates details and stops at `BODY.key === "END"`.
- `iframe-popup.html` embeds the form or opens it in a popup. Results/errors use `postMessage`; the receiver checks origin and source window.

Serve these pages over HTTP from the same origin as eFCSPay (do not open as `file://`). These are integration examples, not production payment forms. Replace mock values, require authenticated sessions, authorize every page request, validate cursor/transaction ownership on the server, use HTTPS, and do not expose secrets or trust postMessage data without server-side validation.

T112 and T201 are sent once in these examples. Do not blindly retry T102 after an ambiguous timeout; follow the host's idempotency/status-query protocol.
