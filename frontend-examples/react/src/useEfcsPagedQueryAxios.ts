import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import axios from "axios";
import type { EfcsCode, EfcsDetail, EfcsRequest, EfcsResponse } from "./useEfcsPagedQuery";

type Options = { prsCode: EfcsCode; request: EfcsRequest; endpoint?: string; enabled?: boolean; pageSize?: number; maxPages?: number };

/** Browser sequentially requests one host page per HTTP call until BODY.key === END. */
export function useEfcsPagedQueryAxios({ prsCode, request, endpoint = "/api/messages", enabled = true, pageSize = 10, maxPages = 100 }: Options) {
  const [response, setResponse] = useState<EfcsResponse | null>(null);
  const [visibleCount, setVisibleCount] = useState(pageSize);
  const [loading, setLoading] = useState(false);
  const [loadingNext, setLoadingNext] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const controllerRef = useRef<AbortController | null>(null);
  const runIdRef = useRef(0);
  const requestJson = useMemo(() => JSON.stringify(request), [request]);
  const detailKey = prsCode === "T101" ? "QUERYDETAIL" : "PAYDETAIL";
  const allRows: EfcsDetail[] = response?.BODY?.[detailKey] ?? [];

  const reload = useCallback(async () => {
    controllerRef.current?.abort();
    const controller = new AbortController(); controllerRef.current = controller;
    const runId = ++runIdRef.current;
    const isCurrent = () => runId === runIdRef.current && !controller.signal.aborted;
    setLoading(true); setLoadingNext(false); setError(null); setResponse(null); setVisibleCount(pageSize);
    let cursor = (JSON.parse(requestJson) as EfcsRequest).BODY.key ?? "";
    const seenKeys = new Set<string>();
    let merged: EfcsResponse | null = null;
    try {
      for (let pageNo = 1; pageNo <= maxPages; pageNo++) {
        if (!isCurrent()) return;
        if (pageNo > 1) setLoadingNext(true);
        const payload = JSON.parse(requestJson) as EfcsRequest; payload.BODY.key = cursor;
        const { data } = await axios.post<EfcsResponse>(`${endpoint}/${prsCode}`, payload, {
          signal: controller.signal, headers: { "Content-Type": "application/json", Accept: "application/json" },
        });
        if (data.HEAD?.prsCode !== prsCode) throw new Error("Response HEAD.prsCode does not match request");
        if (!Array.isArray(data.BODY?.[detailKey]) || typeof data.BODY.key !== "string" || !data.BODY.key) {
          throw new Error(`Response BODY.${detailKey} or continuation key is invalid`);
        }
        merged = merged
          ? { ...data, BODY: { ...data.BODY, [detailKey]: [...(merged.BODY[detailKey] ?? []), ...(data.BODY[detailKey] ?? [])] } }
          : data;
        if (!isCurrent()) return;
        setResponse(merged);
        const nextKey = data.BODY.key;
        if (nextKey === "END") return;
        if (nextKey === cursor || seenKeys.has(nextKey)) throw new Error(`Repeated continuation key: ${nextKey}`);
        seenKeys.add(nextKey); cursor = nextKey;
      }
      throw new Error(`Exceeded maximum page count (${maxPages}) for ${prsCode}`);
    } catch (cause) {
      if (isCurrent() && !axios.isCancel(cause)) {
        setError(axios.isAxiosError(cause) ? String(cause.response?.data?.message ?? cause.message) : cause instanceof Error ? cause.message : "Unknown EFCS error");
      }
    } finally {
      if (isCurrent()) { setLoading(false); setLoadingNext(false); }
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
