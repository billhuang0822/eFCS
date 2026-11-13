# T101/T102 瀏覽器驅動分頁 PR 說明

## 摘要

這次 PR 依照 T101/T102 continuation flow 調整分頁行為，改由瀏覽器主導續頁：

- T101/T102 的續頁流程由瀏覽器依序發動；eFCSPay 每次只代理一個 host page，不在單一 server request 中追蹤完整 cursor 流程。
- 瀏覽器保留同一筆交易資料，更新 `BODY.key` 後自動呼叫下一頁。
- 瀏覽器逐頁合併 T101 的 `QUERYDETAIL` 或 T102 的 `PAYDETAIL`，直到主機回傳 `BODY.key === "END"`。
- React Fetch/Axios、Vue 及 HTML Fetch/Axios/form 範例皆採用 browser-driven 續查流程。
- eFCSPay 驗證交易代碼、請求結構與主機回應，並新增可設定的上游 host connect/read timeout。
- 補充分頁流程、timeout、安全與 T102 交易重送風險說明。

## 為什麼由瀏覽器續頁

若 server 在單一請求中追蹤所有 host pages，browser-to-server request 會持續等待整個查詢完成，可能遇到瀏覽器、gateway、load balancer 或 web server timeout。改由瀏覽器依序發出多個請求，可避免單一 server request 等待整段分頁流程。

每次呼叫仍經由 eFCSPay；瀏覽器不直接連線電文主機。這個設計可縮短每個 browser-to-server request，但單次 host page 呼叫仍可能逾時，因此也設定了上游 timeout。

## 流程

1. Browser 呼叫 `POST /api/messages/{T101|T102}`，初始 `BODY.key` 使用協定規定的起始值。
2. eFCSPay 驗證 URL `prsCode` 與 `HEAD.prsCode` 相同，並只呼叫 host 一次。
3. eFCSPay 回傳該頁明細及 host 回傳的 `BODY.key`。
4. Browser 保留同一筆交易資料，以回傳的 key 依序呼叫下一頁。
5. Browser 收到 `BODY.key === "END"` 後停止並呈現合併結果。

## 實作內容

### Backend

- `EfcsMessageService` 驗證支援的交易代碼、`HEAD` / `BODY` 結構，以及 URL `prsCode` 與 `HEAD.prsCode` 是否一致。
- 每個 browser request 只代理一個 host request，不在服務端追蹤 cursor。
- 驗證主機回應包含相符的 `HEAD.prsCode`；T101/T102 回應需包含明細陣列及非空 continuation key。
- 新增上游 host timeout 設定：
  - `efcs.host.connect-timeout`：預設 `3s`。
  - `efcs.host.read-timeout`：預設 `20s`。
  - 可透過 `EFCS_HOST_CONNECT_TIMEOUT` 與 `EFCS_HOST_READ_TIMEOUT` 環境變數調整。

### Frontend

- T101/T102 由瀏覽器依序自動續查，並在 `BODY.key === "END"` 時停止。
- Browser 端累積每頁的 `QUERYDETAIL` / `PAYDETAIL`。
- 範例加入空 key、重複 key 與最大頁數檢查；支援的前端 hook 也提供 loading、錯誤及取消處理。
- `form.html`、iframe/popup 範例同步採用相同流程。

### 文件

- 說明 browser-driven continuation pagination 的原因、流程及限制。
- 補充分頁協定範例、部署 timeout 與安全注意事項。

## 安全與營運注意事項

- 每頁請求都必須在伺服器端執行 authentication / authorization，並依正式交易規則確認 cursor 與使用者、交易的關聯。不能把 browser-held cursor 當成授權依據。
- 一頁一個請求可降低長時間 request 風險，但單次 host call 仍可能 timeout；connect/read timeout 應依環境及主機 SLA 調整。
- T102 遇到結果不明的 timeout 時，不可盲目重送付款請求；應依主機的冪等或交易狀態查詢規則處理。
- 前端與 server 都應防止空 cursor、重複 cursor 與無限制分頁。
- 正式環境避免記錄完整電文或其他敏感資料。

## 驗證清單

- [ ] `(cd eFCSPay && mvn test)`
- [ ] `(cd mock && mvn test)`
- [ ] 確認 T101/T102 mock 流程逐頁回傳 next key，並在 `END` 停止。
- [ ] 確認 HTML form、iframe/popup、React 與 Vue 範例在第一頁 key 非 `END` 時會自動續查。
- [ ] 確認上游 timeout 設定在 host 延遲情境下能正常觸發。
- [ ] 確認正式部署每個 page request 都執行認證、授權及交易/cursor 綁定驗證。
