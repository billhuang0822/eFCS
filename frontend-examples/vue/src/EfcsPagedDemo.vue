<script setup lang="ts">
import { computed, ref } from "vue";
import { useEfcsPagedQuery, type EfcsCode, type EfcsRequest } from "./useEfcsPagedQuery";

const prsCode = ref<EfcsCode>("T101");
const activeRequest = ref<EfcsRequest | null>(null);
const query = useEfcsPagedQuery(activeRequest, 5);
const columns = computed(() => prsCode.value === "T101"
  ? [["queryDetailNo", "明細號"], ["queryBillData", "帳單資料"], ["queryAmount", "金額"], ["queryDate", "日期"]]
  : [["detailNo", "明細號"], ["billData", "帳單資料"], ["txnAmount", "金額"], ["rtnCodeDesc", "結果"]]);

function submit() {
  const txnTime = new Date().toISOString().replace(/[-:TZ.]/g, "").slice(0, 14);
  const HEAD = { txnChannel: "WEB", prsCode: prsCode.value, txnDataTime: txnTime,
    txnNo: `WEB-${txnTime}`, billerId: "BILLER001", bTypeClass: "", icChkCode: "", icChkDesc: "" };
  if (prsCode.value === "T101") {
    activeRequest.value = { HEAD, BODY: { queryType: "01", queryData1: "A001", queryData2: "",
      queryData3: "", queryData4: "", queryData5: "", eBillSn: "", outBankId: "", key: "" } };
  } else {
    activeRequest.value = { HEAD, BODY: { PAYHEAD: { pendingNo: "MOCK-PENDING-001", totalCount: 1,
      totalAmount: 100, totalFee: 0, memberId: "MEMBER001", billType: "01" },
      PAYDETAIL: [{ detailNo: "1", txnAmount: 100, billData: "MOCK-BILL-1", billBatch: "001" }], key: "" } };
  }
}
</script>

<template>
  <main class="efcs-demo">
    <h2>eFCS 電文分頁展示</h2>
    <label>交易類型
      <select v-model="prsCode" @change="activeRequest = null">
        <option value="T101">T101 查詢</option><option value="T102">T102 繳費</option>
      </select>
    </label>
    <button @click="submit" :disabled="query.loading">{{ query.loading ? "查詢中…" : "送出查詢" }}</button>
    <p v-if="query.loading" aria-live="polite">正在向 eFCSPay 查詢全部電文頁面…</p>
    <p v-if="query.error" role="alert" class="error">{{ query.error }}</p>
    <section v-if="activeRequest && !query.loading && !query.error">
      <p>共 {{ query.total }} 筆；主機分頁已由 eFCSPay 追蹤至 key=END。</p>
      <table>
        <thead><tr><th v-for="col in columns" :key="col[0]">{{ col[1] }}</th></tr></thead>
        <tbody><tr v-for="(row, index) in query.rows" :key="String(row[columns[0][0]]) + index">
          <td v-for="col in columns" :key="col[0]">{{ row[col[0]] ?? "-" }}</td>
        </tr></tbody>
      </table>
      <button v-if="query.hasMore" @click="query.loadMore">下一頁</button>
      <p v-else-if="query.total">已顯示全部資料。</p>
      <p v-else>查無資料。</p>
    </section>
  </main>
</template>

<style scoped>
.efcs-demo { max-width: 960px; margin: 2rem auto; font-family: sans-serif; }
button, select { margin: 0.5rem; padding: 0.4rem 0.7rem; }
table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
th, td { border: 1px solid #ccc; padding: 0.5rem; text-align: left; }
.error { color: #b00020; }
</style>
