# eFCS T101/T102 分頁 PR 說明

## 摘要

- T101/T102 的續頁流程由瀏覽器依序發動，eFCSPay 每次只代理一個 host page，不在單一 server request 內追完整個 cursor 流程。
- React Fetch/Axios、Vue 與 HTML Fetch/Axios/表單範例會帶回應的 `BODY.key` 自動呼叫下一頁，直到 `END`，並聚合 `QUERYDETAIL` 或 `PAYDETAIL`。
- eFCSPay 驗證交易代碼、回應 HEAD 與 T101/T102 page payload，並設定可調整的 host connect/read timeout。
- 補充分頁流程、安全注意事項、timeout 與 T102 交易重送風險說明。

## 為什麼由瀏覽器續頁

在 server 端一次追蹤所有 host pages，會使單一 browser-to-server request 持續等待，可能遇到瀏覽器、gateway、load balancer 或 web server timeout。改成 browser sequential requests 後，每次 server request 只轉送一個 host page；每頁仍有明確的 upstream timeout。

## 流程

1. Browser 呼叫 `POST /api/messages/{T101|T102}`，初始 `BODY.key` 使用協定規定的起始值。
2. eFCSPay 驗證 URL `prsCode` 與 `HEAD.prsCode` 相同，並只呼叫 host 一次。
3. eFCSPay 回傳該頁 details 和 host 回傳的 `BODY.key`。
4. Browser 保留同一筆交易資料，帶上新的 key 依序呼叫下一頁。
5. Browser 收到 `BODY.key === "END"` 後停止並呈現合併結果。

## 風險與限制

- 每頁都需要伺服器端 authentication/authorization，並依正式交易規則確認 cursor 與使用者及交易的關聯。Browser-held cursor 不能作為授權依據。
- 一頁一個請求降低長 request 風險，但單次 host call 仍可能 timeout；connect/read timeout 應按環境調整。
- T102 遇到結果不明的 timeout 時，不可盲目重送；須依主機的冪等或交易狀態查詢規則處理。
- 前端與 server 都應防止空 cursor、重複 cursor 與無限制分頁；避免記錄完整敏感電文內容。

## 驗證清單

- [ ] `(cd eFCSPay && mvn test)`
- [ ] `(cd mock && mvn test)`
- [ ] T101/T102 mock 流程會逐頁回傳 next key，並在 `END` 停止。
- [ ] `frontend-examples/html/form.html`、iframe/popup 與 React/Vue 範例在第一頁非 END key 時會自動續查。
- [ ] 確認正式部署每次 page request 都執行認證、授權及交易/cursor 綁定驗證。
