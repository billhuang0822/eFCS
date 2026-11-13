# React pagination examples

Install Axios for the Axios hook/demo:

```sh
npm install axios
```

## Files

- `src/useEfcsPagedQuery.ts`: Fetch-based hook.
- `src/useEfcsPagedQueryAxios.ts`: Axios-based hook.
- `src/EfcsPagedDemo.tsx` and `src/EfcsPagedDemoAxios.tsx`: T101/T102 table demos.

Both hooks automatically fetch T101/T102 host pages **sequentially from the browser**. Each request sends one page to `POST /api/messages/{prsCode}`; eFCSPay proxies one host page and returns its `BODY.key`. The hook sends the next request with that cursor until it receives `END`, while aggregating `QUERYDETAIL` or `PAYDETAIL`. The UI's local “下一頁” button only controls how many aggregated rows are displayed; it does not initiate a host request.

The hooks enforce a configurable page limit, detect empty/repeated cursors, report loading/errors, and abort outstanding requests on cleanup. A timeout can still affect an individual page request. Do not blindly retry T102 after ambiguous timeout; use the host's idempotency/status-query protocol.

Render either demo component from your app entry point. The examples default to `/api/messages`; configure a same-origin development proxy or pass a service endpoint and configure CORS if the UI is hosted on another origin. Add authentication and server-side authorization for every page request; never rely on client-held cursors as authorization.
