import { useMemo, useState } from "react";
import { EfcsCode, EfcsRequest, useEfcsPagedQuery } from "./useEfcsPagedQuery";

const now = () => new Date().toISOString().replace(/[-:TZ.]/g, "").slice(0, 14);

export default function EfcsPagedDemo() {
  const [prsCode, setPrsCode] = useState<EfcsCode>("T101");
  const [submitted, setSubmitted] = useState(false);
  const request = useMemo<EfcsRequest>(() => {
    const txnTime = now();
    if (prsCode === "T101") {
      return {
        HEAD: { txnChannel: "WEB", prsCode: "T101", txnDataTime: txnTime,
          txnNo: `WEB-${txnTime}`, billerId: "BILLER001", bTypeClass: "", icChkCode: "", icChkDesc: "" },
        BODY: { queryType: "01", queryData1: "A001", queryData2: "", queryData3: "",
          queryData4: "", queryData5: "", eBillSn: "", outBankId: "", key: "" },
      };
    }
    return {
      HEAD: { txnChannel: "WEB", prsCode: "T102", txnDataTime: txnTime,
        txnNo: `WEB-${txnTime}`, billerId: "BILLER001", bTypeClass: "", icChkCode: "", icChkDesc: "" },
      BODY: {
        PAYHEAD: { pendingNo: "MOCK-PENDING-001", pendingNotAfter: "", totalCount: 1,
          totalAmount: 100, totalFee: 0, outBankId: "", memberId: "MEMBER001", payDatetime: txnTime,
          checkType: "", billType: "01", deviceId: "", carrierType: "", carrierNo: "", mobile: "",
          accountNo: "", cardNo: "", merchantId: "", mccCode: "", terminal: "", email: "", querySeqNo: "" },
        PAYDETAIL: [{ detailNo: "1", txnAmount: 100, billData: "MOCK-BILL-1", billBatch: "001" }],
        key: "",
      },
    };
  }, [prsCode]);

  const query = useEfcsPagedQuery({ prsCode, request, enabled: submitted, pageSize: 5 });
  const columns = prsCode === "T101"
    ? [["queryDetailNo", "明細號"], ["queryBillData", "帳單資料"], ["queryAmount", "金額"], ["queryDate", "日期"]]
    : [["detailNo", "明細號"], ["billData", "帳單資料"], ["txnAmount", "金額"], ["rtnCodeDesc", "結果"]];

  return <section style={{ maxWidth: 960, margin: "2rem auto", fontFamily: "sans-serif" }}>
    <h2>eFCS 電文分頁展示</h2>
    <label>交易類型　<select value={prsCode} onChange={(event) => {
      setPrsCode(event.target.value as EfcsCode); setSubmitted(false);
    }}>
      <option value="T101">T101 查詢</option><option value="T102">T102 繳費</option>
    </select></label>
    <button style={{ marginLeft: 12 }} onClick={() => setSubmitted(true)} disabled={query.loading}>
      {query.loading ? "查詢中…" : "送出查詢"}
    </button>
    {query.error && <p role="alert" style={{ color: "#b00020" }}>{query.error}</p>}
    {query.loading && <p aria-live="polite">正在向 eFCSPay 查詢全部電文頁面…</p>}
    {submitted && !query.loading && !query.error && <>
      <p>共 {query.total} 筆；主機分頁已由 eFCSPay 追蹤至 key=END。</p>
      <table style={{ borderCollapse: "collapse", width: "100%" }}>
        <thead><tr>{columns.map(([key, label]) => <th key={key} style={cell}>{label}</th>)}</tr></thead>
        <tbody>{query.rows.map((row, index) => <tr key={`${String(row[columns[0][0]])}-${index}`}>
          {columns.map(([key]) => <td key={key} style={cell}>{row[key] ?? "-"}</td>)}
        </tr>)}</tbody>
      </table>
      {query.hasMore && <button onClick={query.loadMore} style={{ marginTop: 12 }}>下一頁</button>}
      {!query.hasMore && query.total > 0 && <p>已顯示全部資料。</p>}
      {query.total === 0 && <p>查無資料。</p>}
    </>}
  </section>;
}

const cell: React.CSSProperties = { border: "1px solid #ccc", padding: 8, textAlign: "left" };
