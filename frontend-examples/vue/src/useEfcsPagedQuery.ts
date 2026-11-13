import { computed, ref, watch } from "vue";

export type EfcsCode = "T101" | "T102";
export type EfcsRequest = { HEAD: { txnChannel: string; prsCode: EfcsCode; txnDataTime: string; txnNo: string; billerId: string; bTypeClass: string; icChkCode: string; icChkDesc: string }; BODY: Record<string, any> & { key: string } };
type Detail = Record<string, string | number | null | undefined>;
type Response = { HEAD: { prsCode: EfcsCode }; BODY: { key: string; QUERYDETAIL?: Detail[]; PAYDETAIL?: Detail[]; [key: string]: any } };

/** Browser advances one host cursor per request; the service proxies a single page. */
export function useEfcsPagedQuery(request = ref<EfcsRequest | null>(null)) {
  const response = ref<Response | null>(null); const loading = ref(false); const loadingNext = ref(false); const error = ref<string | null>(null);
  const code = computed(() => request.value?.HEAD.prsCode ?? "T101");
  const detailName = computed(() => code.value === "T101" ? "QUERYDETAIL" : "PAYDETAIL");
  const allRows = computed(() => response.value?.BODY?.[detailName.value] ?? []);
  const hasMore = computed(() => Boolean(response.value && response.value.BODY.key !== "END"));
  let cursor = ""; let sequence = 0; let busy = false;

  async function fetchPage(key: string, append: boolean, current: number) {
    if (!request.value) return;
    const payload = JSON.parse(JSON.stringify(request.value)); payload.BODY.key = key;
    const result = await fetch(`/api/messages/${code.value}`, { method: "POST", headers: { "Content-Type": "application/json", Accept: "application/json" }, body: JSON.stringify(payload) });
    if (!result.ok) throw new Error(`EFCS ${code.value} failed (${result.status}): ${await result.text()}`);
    const page = await result.json() as Response;
    if (page.HEAD?.prsCode !== code.value || !Array.isArray(page.BODY?.[detailName.value]) || !page.BODY.key) throw new Error("Invalid eFCS page response");
    if (current !== sequence) return;
    cursor = page.BODY.key;
    response.value = append && response.value
      ? { ...page, BODY: { ...page.BODY, [detailName.value]: [...(response.value.BODY[detailName.value] ?? []), ...(page.BODY[detailName.value] ?? [])] } }
      : page;
  }
  async function reload() {
    if (!request.value) return;
    const current = ++sequence; busy = true; cursor = ""; loading.value = true; loadingNext.value = false; error.value = null; response.value = null;
    try { await fetchPage("", false, current); }
    catch (cause) { if (current === sequence) error.value = cause instanceof Error ? cause.message : "Unknown EFCS error"; }
    finally { if (current === sequence) { busy = false; loading.value = false; } }
  }
  async function loadMore() {
    if (busy || !cursor || cursor === "END") return;
    const current = sequence; busy = true; loadingNext.value = true; error.value = null;
    try { await fetchPage(cursor, true, current); }
    catch (cause) { error.value = cause instanceof Error ? cause.message : "Unknown EFCS error"; }
    finally { busy = false; loadingNext.value = false; }
  }
  watch(request, () => { void reload(); }, { deep: true });
  return { code, response, rows: allRows, total: computed(() => allRows.value.length), loading, loadingNext, error, hasMore, loadMore, reload };
}
