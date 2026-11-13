import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import axios from "axios";
import type { EfcsCode, EfcsDetail, EfcsRequest, EfcsResponse } from "./useEfcsPagedQuery";

type Options = { prsCode: EfcsCode; request: EfcsRequest; endpoint?: string; enabled?: boolean };

/** Each loadMore starts one browser -> service -> host request with the returned cursor. */
export function useEfcsPagedQueryAxios({ prsCode, request, endpoint = "/api/messages", enabled = true }: Options) {
  const [response, setResponse] = useState<EfcsResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadingNext, setLoadingNext] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const cursorRef = useRef(""); const busyRef = useRef(false);
  const requestJson = useMemo(() => JSON.stringify(request), [request]);
  const detailKey = prsCode === "T101" ? "QUERYDETAIL" : "PAYDETAIL";
  const rows: EfcsDetail[] = response?.BODY?.[detailKey] ?? [];

  const fetchPage = useCallback(async (cursor: string, signal?: AbortSignal, append = false) => {
    const payload = JSON.parse(requestJson) as EfcsRequest;
    payload.BODY.key = cursor;
    const { data } = await axios.post<EfcsResponse>(`${endpoint}/${prsCode}`, payload, { signal,
      headers: { "Content-Type": "application/json", Accept: "application/json" } });
    if (data.HEAD?.prsCode !== prsCode) throw new Error("Response HEAD.prsCode does not match request");
    if (!Array.isArray(data.BODY?.[detailKey]) || typeof data.BODY.key !== "string" || !data.BODY.key) {
      throw new Error(`Response BODY.${detailKey} or continuation key is invalid`);
    }
    cursorRef.current = data.BODY.key;
    setResponse((previous) => !append || !previous ? data : { ...data, BODY: { ...data.BODY,
      [detailKey]: [...(previous.BODY[detailKey] ?? []), ...(data.BODY[detailKey] ?? [])] } });
  }, [detailKey, endpoint, prsCode, requestJson]);

  const reload = useCallback(async (signal?: AbortSignal) => {
    busyRef.current = true; setLoading(true); setLoadingNext(false); setError(null); setResponse(null); cursorRef.current = "";
    try { await fetchPage("", signal, false); }
    catch (cause) { if (!signal?.aborted && !axios.isCancel(cause)) setError(axios.isAxiosError(cause) ? String(cause.response?.data?.message ?? cause.message) : cause instanceof Error ? cause.message : "Unknown EFCS error"); }
    finally { busyRef.current = false; if (!signal?.aborted) setLoading(false); }
  }, [fetchPage]);
  useEffect(() => { if (!enabled) return; const controller = new AbortController(); void reload(controller.signal); return () => controller.abort(); }, [enabled, reload]);

  const loadMore = useCallback(async () => {
    if (busyRef.current || !cursorRef.current || cursorRef.current === "END") return;
    busyRef.current = true; setLoadingNext(true); setError(null);
    try { await fetchPage(cursorRef.current, undefined, true); }
    catch (cause) { setError(axios.isAxiosError(cause) ? String(cause.response?.data?.message ?? cause.message) : cause instanceof Error ? cause.message : "Unknown EFCS error"); }
    finally { busyRef.current = false; setLoadingNext(false); }
  }, [fetchPage]);

  return { response, rows, total: rows.length, loading, loadingNext, error,
    hasMore: Boolean(response && response.BODY.key !== "END"), loadMore, reload: () => reload() };
}
