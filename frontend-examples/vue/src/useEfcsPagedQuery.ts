import { computed, ref, watch } from "vue";

export type EfcsCode = "T101" | "T102";
export type EfcsRequest = { HEAD: { txnChannel: string; prsCode: EfcsCode; txnDataTime: string; txnNo: string; billerId: string; bTypeClass: string; icChkCode: string; icChkDesc: string }; BODY: Record<string, any> & { key: string } };
type Detail = Record<string, string | number | null | undefined>;
type Response = { HEAD: { prsCode: EfcsCode }; BODY: { key: string; QUERYDETAIL?: Detail[]; PAYDETAIL?: Detail[]; [key: string]: any } };

/** Automatically fetches host pages sequentially from the browser; backend proxies one page per call. */
export function useEfcsPagedQuery(request = ref<EfcsRequest | null>(null), pageSize = 10, maxPages = 100) {
  const response = ref<Response | null>(null); const loading = ref(false); const loadingNext = ref(false); const error = ref<string | null>(null);
  const code = computed(() => request.value?.HEAD.prsCode ?? "T101");
  const detailName = computed(() => code.value === "T101" ? "QUERYDETAIL" : "PAYDETAIL");
  const allRows = computed(() => response.value?.BODY?.[detailName.value] ?? []);
  const visibleCount = ref(pageSize);
  const rows = computed(() => allRows.value.slice(0, visibleCount.value));
  const hasMore = computed(() => visibleCount.value > 0 && visibleCount.value < allRows.value.length);
  let sequence = 0; let controller: AbortController | null = null;

  async function reload() {
    if (!request.value) return;
    controller?.abort(); controller = new AbortController();
    const current = ++sequence; const signal = controller.signal;
    loading.value = true; loadingNext.value = false; error.value = null; response.value = null; visibleCount.value = pageSize;
    let cursor = request.value.BODY.key ?? ""; const seen = new Set<string>(); let merged: Response | null = null;
    try {
      for (let pageNo = 1; pageNo <= maxPages; pageNo++) {
        if (signal.aborted || current !== sequence) return;
        if (pageNo > 1) loadingNext.value = true;
        const payload = JSON.parse(JSON.stringify(request.value)) as EfcsRequest; payload.BODY.key = cursor;
        const result = await fetch(`/api/messages/${code.value}`, { method: "POST", signal, headers: { "Content-Type": "application/json", Accept: "application/json" }, body: JSON.stringify(payload) });
        if (!result.ok) throw new Error(`EFCS ${code.value} page ${pageNo} failed (${result.status}): ${await result.text()}`);
        const page = await result.json() as Response;
        if (page.HEAD?.prsCode !== code.value || !Array.isArray(page.BODY?.[detailName.value]) || typeof page.BODY.key !== "string" || !page.BODY.key) throw new Error("Invalid eFCS page response or continuation key");
        merged = merged ? { ...page, BODY: { ...page.BODY, [detailName.value]: [...(merged.BODY[detailName.value] ?? []), ...(page.BODY[detailName.value] ?? [])] } } : page;
        response.value = merged;
        const nextKey = page.BODY.key;
        if (nextKey === "END") return;
        if (nextKey === cursor || seen.has(nextKey)) throw new Error(`Repeated continuation key: ${nextKey}`);
        seen.add(nextKey); cursor = nextKey;
      }
      throw new Error(`Exceeded maximum page count (${maxPages}) for ${code.value}`);
    } catch (cause) {
      if (!signal.aborted && current === sequence) error.value = cause instanceof Error ? cause.message : "Unknown EFCS error";
    } finally {
      if (!signal.aborted && current === sequence) { loading.value = false; loadingNext.value = false; }
    }
  }
  function loadMore() { visibleCount.value = Math.min(visibleCount.value + pageSize, allRows.value.length); }
  watch(request, () => { void reload(); }, { deep: true });
  return { code, response, rows, total: computed(() => allRows.value.length), loading, loadingNext, error, hasMore, loadMore, reload };
}
