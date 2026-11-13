# HTML form and iframe/popup demos

- `form.html` is an asynchronous form with T101, T102, T112 and T201 sample fields. It posts JSON to `POST /api/messages/{prsCode}` with Fetch and renders the response.
- `iframe-popup.html` embeds the form in an iframe or opens it in a popup. Results/errors are sent to the opener/parent with `postMessage`; the receiver checks origin and source window.

Serve these pages over HTTP from the same origin as eFCSPay (do not open as `file://`). These are integration examples, not production payment forms. Replace all sample values with validated application values, use HTTPS and authenticated sessions, and do not expose secrets or trust postMessage data without server-side validation.

Protocol note: T101/T102 expect eFCSPay to follow host continuation keys until `BODY.key === "END"` and return aggregated detail arrays. T112's supplied response sample contains HEAD only; T201 has a BODY containing totalAmount/totalFee.
