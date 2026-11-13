import { computed, ref, watch } from "vue";

export type EfcsCode = "T101" | "T102";
export type EfcsRequest = {
  HEAD: { txnChannel: string; prsCode: EfcsCode; txnDataTime: string; txnNo: string;
    billerId: string; bTypeClass: string; icChkCode: string; icChkDesc: string };
  BODY: Record<string, unknown> & { key: string };
};
type Detail = Record<string, string | number | null | undefined>;
type Response = { HEAD: { prsCode: EfcsCode }; BODY: { key: string; QUERYDETAIL?: Detail[]; PAYDETAIL?: Detail[] } };

/** eFCSPay follows host cursors and returns one merged BODY ending with key=END; this composable paginates display rows locally. */
export function useEfcsPagedQuery(request = ref<EfcsRequest | null>(null), pageSize = 10) {
  const response = ref<Response | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const visibleCount = ref(pageSize);
  const code = computed(() => request.value?.HEAD.prsCode ?? "T101");
  const allRows = computed(() => {
    const body = response.value?.BODY;
    return code.value === "T101" ? body?.QUERYDETAIL ?? [] : body?.PAYDETAIL ?? [];
  });
  const rows = computed(() => allRows.value.slice(0, visibleCount.value));
  const hasMore = computed(() => visibleCount.value < allRows.value.length);
  let sequence = 0;

  async function reload() {
    if (!request.value) return;
    const current = ++sequence;
    loading.value = true; error.value = null; response.value = null; visibleCount.value = pageSize;
    try {
      const result = await fetch(`/api/messages/${code.value}`, {
        method: "POST", headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify(request.value),
      });
      if (!result.ok) throw new Error(`EFCS ${code.value} failed (${result.status}): ${await result.text()}`);
      const data = await result.json() as Response;
      if (data.HEAD?.prsCode !== code.value) throw new Error("Response HEAD.prsCode does not match request");
      if (data.BODY?.key !== "END") throw new Error('Expected merged eFCSPay response with BODY.key="END"');
      const key = code.value === "T101" ? "QUERYDETAIL" : "PAYDETAIL";
      if (!Array.isArray(data.BODY[key])) throw new Error(`Response BODY.${key} is missing or invalid`);
      if (current === sequence) response.value = data;
    } catch (cause) {
      if (current === sequence) error.value = cause instanceof Error ? cause.message : "Unknown EFCS error";
    } finally {
      if (current === sequence) loading.value = false;
    }
  }

  function loadMore() { visibleCount.value = Math.min(visibleCount.value + pageSize, allRows.value.length); }
  watch(request, () => { void reload(); }, { deep: true });
  return { code, response, rows, total: computed(() => allRows.value.length), loading, error, hasMore, loadMore, reload };
}
