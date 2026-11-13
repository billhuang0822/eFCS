import { useMemo, useState } from "react";
import type { EfcsCode, EfcsRequest } from "./useEfcsPagedQuery";
import { useEfcsPagedQueryAxios } from "./useEfcsPagedQueryAxios";

const now = () => new Date().toISOString().replace(/[-:TZ.]/g, "").slice(0, 14);

export default function EfcsPagedDemoAxios() {
  const [prsCode, setPrsCode] = useState<EfcsCode>("T101");
  const [submitted, setSubmitted] = useState(false);
  const request = useMemo<EfcsRequest>(() => {
    const time = now();
    const HEAD = { txnChannel: "WEB", prsCode, txnDataTime: time, txnNo: `WEB-${time}`,
      billerId: "BILLER001", bTypeClass: "", icChkCode: "", icChkDesc: "" };
    if (prsCode === "T101") return { HEAD, BODY: { queryType: "01", queryData1: "A001",
      queryData2: "", queryData3: "", queryData4: "", queryData5: "", eBillSn: "", outBankId: "", key: "" } };
    return { HEAD, BODY: { PAYHEAD: { pendingNo: "MOCK-PENDING-001", totalCount: 1,
      totalAmount: 100, totalFee: 0, memberId: "MEMBER001", billType: "01" },
      PAYDETAIL: [{ detailNo: "1", txnAmount: 100, billData: "MOCK-BILL-1", billBatch: "001" }], key: "" } };
  }, [prsCode]);
  const query = useEfcsPagedQueryAxios({ prsCode, request, enabled: submitted, pageSize: 5 });
  const columns = prsCode === "T101"
    ? [["queryDetailNo", "明細號"], ["queryBillData", "帳單資料"], ["queryAmount", "金額"], ["queryDate", "日期"]]
    : [["detailNo", "明細號"], ["billData", "帳單資料"], ["txnAmount", "金額"], ["rtnCodeDesc", "結果"]];

  return <section style={{ maxWidth: 960, margin: "2rem auto", fontFamily: "sans-serif" }}>
    <h2>eFCS 分頁展示（Axios）</h2>
    <label>交易類型　<select value={prsCode} onChange={(event) => {
      setPrsCode(event.target.value as EfcsCode); setSubmitted(false);
    }}><option value="T101">T101 查詢</option><option value="T102">T102 繳費</option></select></label>
    <button style={{ marginLeft: 12 }} onClick={() => setSubmitted(true)} disabled={query.loading}>
      {query.loading ? "查詢中…" : "送出查詢"}
    </button>
    {query.loading && <p aria-live="polite">正在呼叫 eFCSPay，等待主機分頁完成…</p>}
    {query.error && <p role="alert" style={{ color: "#b00020" }}>{query.error}</p>}
    {submitted && !query.loading && !query.error && <>
      <p>共 {query.total} 筆；後端已追蹤主機 key 至 END。</p>
      <table style={{ borderCollapse: "collapse", width: "100%" }}>
        <thead><tr>{columns.map(([key, title]) => <th key={key} style={cell}>{title}</th>)}</tr></thead>
        <tbody>{query.rows.map((row, i) => <tr key={`${String(row[columns[0][0]])}-${i}`}>
          {columns.map(([key]) => <td key={key} style={cell}>{row[key] ?? "-"}</td>)}
        </tr>)}</tbody>
      </table>
      {query.hasMore ? <button style={{ marginTop: 12 }} onClick={query.loadMore}>下一頁</button>
        : <p>{query.total ? "已顯示全部資料。" : "查無資料。"}</p>}
    </>}
  </section>;
}

const cell: React.CSSProperties = { border: "1px solid #ccc", padding: 8, textAlign: "left" };
