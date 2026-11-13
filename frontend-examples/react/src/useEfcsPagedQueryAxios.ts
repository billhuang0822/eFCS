import { useCallback, useEffect, useMemo, useState } from "react";
import axios from "axios";
import type { EfcsCode, EfcsDetail, EfcsRequest, EfcsResponse } from "./useEfcsPagedQuery";

type Options = {
  prsCode: EfcsCode;
  request: EfcsRequest;
  endpoint?: string;
  enabled?: boolean;
  pageSize?: number;
};

/**
 * Axios version of the eFCSPay hook. The backend follows host continuation keys
 * until END and returns the aggregated list; loadMore paginates that list locally.
 */
export function useEfcsPagedQueryAxios({
  prsCode,
  request,
  endpoint = "/api/messages",
  enabled = true,
  pageSize = 10,
}: Options) {
  const [response, setResponse] = useState<EfcsResponse | null>(null);
  const [visibleCount, setVisibleCount] = useState(pageSize);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const requestJson = useMemo(() => JSON.stringify(request), [request]);
  const detailKey = prsCode === "T101" ? "QUERYDETAIL" : "PAYDETAIL";
  const allRows: EfcsDetail[] = response?.BODY?.[detailKey] ?? [];

  const reload = useCallback(async (signal?: AbortSignal) => {
    setLoading(true);
    setError(null);
    setResponse(null);
    setVisibleCount(pageSize);
    try {
      const { data } = await axios.post<EfcsResponse>(
        `${endpoint}/${prsCode}`,
        requestJson,
        {
          signal,
          headers: { "Content-Type": "application/json", Accept: "application/json" },
          transformRequest: [(body) => body],
        },
      );
      if (data.HEAD?.prsCode !== prsCode) throw new Error("Response HEAD.prsCode does not match request");
      if (data.BODY?.key !== "END") throw new Error('Expected aggregated response with BODY.key="END"');
      if (!Array.isArray(data.BODY[detailKey])) throw new Error(`Response BODY.${detailKey} is missing or invalid`);
      setResponse(data);
    } catch (cause) {
      if (!axios.isCancel(cause)) {
        const message = axios.isAxiosError(cause)
          ? (cause.response?.data?.message ?? cause.message)
          : cause instanceof Error ? cause.message : "Unknown EFCS error";
        setError(String(message));
      }
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, [detailKey, endpoint, pageSize, prsCode, requestJson]);

  useEffect(() => {
    if (!enabled) return;
    const controller = new AbortController();
    void reload(controller.signal);
    return () => controller.abort();
  }, [enabled, reload]);

  const loadMore = useCallback(() => {
    setVisibleCount((count) => Math.min(count + pageSize, allRows.length));
  }, [allRows.length, pageSize]);

  return {
    response,
    rows: allRows.slice(0, visibleCount),
    total: allRows.length,
    loading,
    error,
    hasMore: visibleCount < allRows.length,
    loadMore,
    reload: () => reload(),
  };
}
