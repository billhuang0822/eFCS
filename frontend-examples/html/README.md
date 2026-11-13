# Plain HTML demos

Serve `fetch.html`, `axios.html`, `form.html`, and `iframe-popup.html` over HTTP. Do not open them as `file://`. Use the same origin as eFCSPay or configure a development proxy/CORS.

For T101/T102, the Fetch/Axios and form demos automatically issue sequential cursor requests from the browser. Each call to `POST /api/messages/{prsCode}` causes eFCSPay to proxy exactly one host page. The browser carries the response `BODY.key` to the next request and stops at `END`. The demo then aggregates the page details. This is different from local “show more” display behavior, which only reveals already-fetched rows.

`axios.html` loads Axios 1.7.9 from jsDelivr. For offline or production use, install Axios in the frontend project and bundle it instead of relying on the CDN. These are integration examples, not production payment forms; replace mock values and apply authentication, authorization, validation, HTTPS, and sensitive-data protections.
