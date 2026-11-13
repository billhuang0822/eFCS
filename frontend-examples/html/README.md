# Plain HTML demos

Open `fetch.html` or `axios.html` from the same origin as eFCSPay, or serve this directory with a local web server and configure a development proxy/CORS.

Both demos call `POST /api/messages/{T101|T102}` once. eFCSPay follows host continuation keys until `BODY.key` is `END` and combines the response details. The **下一頁** button only reveals the next batch from the combined list in the browser; it does not call the host again.

`axios.html` loads Axios 1.7.9 from jsDelivr. For offline or production use, install Axios in the frontend project and bundle it instead of relying on the CDN.
