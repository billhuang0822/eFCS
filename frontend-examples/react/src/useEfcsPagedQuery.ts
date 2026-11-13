import { useCallback, useEffect, useMemo, useRef, useState } from "react";

export type EfcsCode = "T101" | "T102";
export type EfcsHead = { txnChannel: string; prsCode: EfcsCode; txnDataTime: string; txnNo: string; billerId: string; bTypeClass: string; icChkCode: string; icChkDesc: string };
export type T101Request = { HEAD: EfcsHead & { prsCode: "T101" }; BODY: { queryType: string; queryData1: string; queryData2: string; queryData3: string; queryData4: string; queryData5: string; eBillSn: string; outBankId: string; key: string } };
export type T102Request = { HEAD: EfcsHead & { prsCode: "T102" }; BODY: { PAYHEAD: Record<string, string | number>; PAYDETAIL: Array<{ detailNo: string; txnAmount: number; billData: string; billBatch: string }>; key: string } };
export type EfcsRequest = T101Request | T102Request;
export type EfcsDetail = Record<string, string | number | null | undefined>;
export type EfcsResponse = { HEAD: { prsCode: EfcsCode; icChkCode?: string; icChkDesc?: string; [key: string]: unknown }; BODY: { key: string; QUERYHEAD?: Record<string, unknown>; QUERYDETAIL?: EfcsDetail[]; PAYHEAD?: Record<string, unknown>; PAYDETAIL?: EfcsDetail[] } };

type Options = { prsCode: EfcsCode; request: EfcsRequest; endpoint?: string; enabled?: boolean; pageSize?: number; maxPages?: number };

/** Browser sequentially requests one host page per HTTP call until BODY.key === END. */
export function useEfcsPagedQuery({ prsCode, request, endpoint = "/api/messages", enabled = true, pageSize = 10, maxPages = 100 }: Options) {
  const [response, setResponse] = useState<EfcsResponse | null>(null);
  const [visibleCount, setVisibleCount] = useState(pageSize);
  const [loading, setLoading] = useState(false);
  const [loadingNext, setLoadingNext] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const requestJson = useMemo(() => JSON.stringify(request), [request]);
  const detailKey = prsCode === "T101" ? "QUERYDETAIL" : "PAYDETAIL";
  const allRows = response?.BODY?.[detailKey] ?? [];
  const controllerRef = useRef<AbortController | null>(null);
  const runIdRef = useRef(0);
  const busyRef = useRef(false);

  const reload = useCallback(async () => {
    controllerRef.current?.abort();
    const controller = new AbortController();
    controllerRef.current = controller;
    const runId = ++runIdRef.current;
    const isCurrent = () => runId === runIdRef.current && !controller.signal.aborted;
    busyRef.current = true;
    setLoading(true); setLoadingNext(false); setError(null); setResponse(null); setVisibleCount(pageSize);
    let cursor = (JSON.parse(requestJson) as EfcsRequest).BODY.key ?? "";
    const seenKeys = new Set<string>();
    let merged: EfcsResponse | null = null;
    try {
      for (let pageNo = 1; pageNo <= maxPages; pageNo++) {
        if (!isCurrent()) return;
        if (pageNo > 1) setLoadingNext(true);
        const payload = JSON.parse(requestJson) as EfcsRequest;
        payload.BODY.key = cursor;
        const result = await fetch(`${endpoint}/${prsCode}`, {
          method: "POST", headers: { "Content-Type": "application/json", Accept: "application/json" },
          body: JSON.stringify(payload), signal: controller.signal,
        });
        if (!result.ok) throw new Error(`EFCS ${prsCode} page ${pageNo} failed (${result.status}): ${await result.text()}`);
        const page = await result.json() as EfcsResponse;
        if (page.HEAD?.prsCode !== prsCode) throw new Error("Response HEAD.prsCode does not match request");
        if (!Array.isArray(page.BODY?.[detailKey]) || typeof page.BODY.key !== "string" || !page.BODY.key) {
          throw new Error(`Response BODY.${detailKey} or continuation key is invalid`);
        }
        merged = merged
          ? { ...page, BODY: { ...page.BODY, [detailKey]: [...(merged.BODY[detailKey] ?? []), ...(page.BODY[detailKey] ?? [])] } }
          : page;
        if (!isCurrent()) return;
        setResponse(merged);
        const nextKey = page.BODY.key;
        if (nextKey === "END") return;
        if (nextKey === cursor || seenKeys.has(nextKey)) throw new Error(`Repeated continuation key: ${nextKey}`);
        seenKeys.add(nextKey);
        cursor = nextKey;
      }
      throw new Error(`Exceeded maximum page count (${maxPages}) for ${prsCode}`);
    } catch (cause) {
      if (isCurrent()) setError(cause instanceof Error ? cause.message : "Unknown EFCS error");
    } finally {
      if (isCurrent()) { busyRef.current = false; setLoading(false); setLoadingNext(false); }
    }
  }, [detailKey, endpoint, maxPages, pageSize, prsCode, requestJson]);

  useEffect(() => {
    if (!enabled) return;
    void reload();
    return () => { controllerRef.current?.abort(); };
  }, [enabled, reload]);

  const loadMore = useCallback(() => setVisibleCount((count) => Math.min(count + pageSize, allRows.length)), [allRows.length, pageSize]);
  return { response, rows: allRows.slice(0, visibleCount), total: allRows.length, loading, loadingNext, error,
    hasMore: visibleCount < allRows.length, loadMore, reload };
}
