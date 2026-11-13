import { useCallback, useEffect, useMemo, useRef, useState } from "react";

export type EfcsCode = "T101" | "T102";
export type EfcsHead = { txnChannel: string; prsCode: EfcsCode; txnDataTime: string; txnNo: string; billerId: string; bTypeClass: string; icChkCode: string; icChkDesc: string };
export type T101Request = { HEAD: EfcsHead & { prsCode: "T101" }; BODY: { queryType: string; queryData1: string; queryData2: string; queryData3: string; queryData4: string; queryData5: string; eBillSn: string; outBankId: string; key: string } };
export type T102Request = { HEAD: EfcsHead & { prsCode: "T102" }; BODY: { PAYHEAD: Record<string, string | number>; PAYDETAIL: Array<{ detailNo: string; txnAmount: number; billData: string; billBatch: string }>; key: string } };
export type EfcsRequest = T101Request | T102Request;
export type EfcsDetail = Record<string, string | number | null | undefined>;
export type EfcsResponse = { HEAD: { prsCode: EfcsCode; icChkCode?: string; icChkDesc?: string; [key: string]: unknown }; BODY: { key: string; QUERYHEAD?: Record<string, unknown>; QUERYDETAIL?: EfcsDetail[]; PAYHEAD?: Record<string, unknown>; PAYDETAIL?: EfcsDetail[] } };

type Options = { prsCode: EfcsCode; request: EfcsRequest; endpoint?: string; enabled?: boolean };

/** Each loadMore starts one browser -> service -> host request with the returned cursor. */
export function useEfcsPagedQuery({ prsCode, request, endpoint = "/api/messages", enabled = true }: Options) {
  const [response, setResponse] = useState<EfcsResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadingNext, setLoadingNext] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const cursorRef = useRef("");
  const busyRef = useRef(false);
  const requestJson = useMemo(() => JSON.stringify(request), [request]);
  const detailKey = prsCode === "T101" ? "QUERYDETAIL" : "PAYDETAIL";
  const rows = response?.BODY?.[detailKey] ?? [];
  const hasMore = Boolean(response && response.BODY.key !== "END");

  const fetchPage = useCallback(async (cursor: string, signal?: AbortSignal, append = false) => {
    const parsed = JSON.parse(requestJson) as EfcsRequest;
    parsed.BODY.key = cursor;
    const result = await fetch(`${endpoint}/${prsCode}`, { method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(parsed), signal });
    if (!result.ok) throw new Error(`EFCS ${prsCode} failed (${result.status}): ${await result.text()}`);
    const page = await result.json() as EfcsResponse;
    if (page.HEAD?.prsCode !== prsCode) throw new Error("Response HEAD.prsCode does not match request");
    if (!Array.isArray(page.BODY?.[detailKey]) || typeof page.BODY.key !== "string" || !page.BODY.key) {
      throw new Error(`Response BODY.${detailKey} or continuation key is invalid`);
    }
    cursorRef.current = page.BODY.key;
    setResponse((previous) => {
      if (!append || !previous) return page;
      return { ...page, BODY: { ...page.BODY,
        [detailKey]: [...(previous.BODY[detailKey] ?? []), ...(page.BODY[detailKey] ?? [])] } };
    });
  }, [detailKey, endpoint, prsCode, requestJson]);

  const reload = useCallback(async (signal?: AbortSignal) => {
    busyRef.current = true; setLoading(true); setLoadingNext(false); setError(null); setResponse(null); cursorRef.current = "";
    try { await fetchPage("", signal, false); }
    catch (cause) { if (!signal?.aborted) setError(cause instanceof Error ? cause.message : "Unknown EFCS error"); }
    finally { busyRef.current = false; if (!signal?.aborted) setLoading(false); }
  }, [fetchPage]);

  useEffect(() => {
    if (!enabled) return;
    const controller = new AbortController(); void reload(controller.signal);
    return () => controller.abort();
  }, [enabled, reload]);

  const loadMore = useCallback(async () => {
    if (busyRef.current || !cursorRef.current || cursorRef.current === "END") return;
    busyRef.current = true; setLoadingNext(true); setError(null);
    try { await fetchPage(cursorRef.current, undefined, true); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Unknown EFCS error"); }
    finally { busyRef.current = false; setLoadingNext(false); }
  }, [fetchPage]);

  return { response, rows, total: rows.length, loading, loadingNext, error, hasMore, loadMore, reload: () => reload() };
}
