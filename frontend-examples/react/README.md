# React Axios example

Install Axios in the existing React application:

```sh
npm install axios
```

Files:
- `src/useEfcsPagedQueryAxios.ts`: Axios hook with cancellation, loading/error states, validation that eFCSPay returns `BODY.key === "END"`, and local display pagination.
- `src/EfcsPagedDemoAxios.tsx`: T101/T102 table demo with a load-more button.

Import and render `EfcsPagedDemoAxios` from your app entry point. The browser sends one request to `POST /api/messages/{prsCode}`. eFCSPay is responsible for repeatedly calling the host with each continuation key until `END`; the demo's **下一頁** button only reveals more of the already-aggregated rows and does not send another host request.

If the frontend is hosted on another origin, configure a development proxy or set `endpoint` to the service URL and configure CORS on the service.
