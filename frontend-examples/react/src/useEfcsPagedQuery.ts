import { useCallback, useEffect, useMemo, useState } from "react";

export type EfcsCode = "T101" | "T102";
export type EfcsHead = {
  txnChannel: string;
  prsCode: EfcsCode;
  txnDataTime: string;
  txnNo: string;
  billerId: string;
  bTypeClass: string;
  icChkCode: string;
  icChkDesc: string;
};
export type T101Request = {
  HEAD: EfcsHead & { prsCode: "T101" };
  BODY: {
    queryType: string; queryData1: string; queryData2: string; queryData3: string;
    queryData4: string; queryData5: string; eBillSn: string; outBankId: string; key: string;
  };
};
export type T102Request = {
  HEAD: EfcsHead & { prsCode: "T102" };
  BODY: {
    PAYHEAD: Record<string, string | number>;
    PAYDETAIL: Array<{ detailNo: string; txnAmount: number; billData: string; billBatch: string }>;
    key: string;
  };
};
export type EfcsRequest = T101Request | T102Request;
export type EfcsDetail = Record<string, string | number | null | undefined>;
export type EfcsResponse = {
  HEAD: { prsCode: EfcsCode; icChkCode?: string; icChkDesc?: string; [key: string]: unknown };
  BODY: {
    key: string;
    QUERYHEAD?: Record<string, unknown>;
    QUERYDETAIL?: EfcsDetail[];
    PAYHEAD?: Record<string, unknown>;
    PAYDETAIL?: EfcsDetail[];
  };
};

type Options = {
  prsCode: EfcsCode;
  request: EfcsRequest;
  endpoint?: string;
  enabled?: boolean;
  pageSize?: number;
};

/**
 * eFCSPay follows host continuation keys server-side and returns one merged response
 * ending in BODY.key === "END". This hook fetches that result once, then paginates
 * the rendered rows locally. It intentionally does not send cursor requests itself.
 */
export function useEfcsPagedQuery({
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
  const rows = response?.BODY?.[detailKey] ?? [];
  const visibleRows = rows.slice(0, visibleCount);
  const hasMore = visibleCount < rows.length;

  const reload = useCallback(async (signal?: AbortSignal) => {
    setLoading(true);
    setError(null);
    setResponse(null);
    setVisibleCount(pageSize);
    try {
      const result = await fetch(`${endpoint}/${prsCode}`, {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: requestJson,
        signal,
      });
      if (!result.ok) {
        const detail = await result.text();
        throw new Error(`EFCS ${prsCode} failed (${result.status}): ${detail}`);
      }
      const data = (await result.json()) as EfcsResponse;
      if (data.HEAD?.prsCode !== prsCode) throw new Error("Response HEAD.prsCode does not match request");
      if (data.BODY?.key !== "END") {
        throw new Error('Expected eFCSPay aggregated response with BODY.key="END"');
      }
      const listKey = prsCode === "T101" ? "QUERYDETAIL" : "PAYDETAIL";
      if (!Array.isArray(data.BODY[listKey])) throw new Error(`Response BODY.${listKey} is missing or invalid`);
      setResponse(data);
    } catch (cause) {
      if (!(cause instanceof DOMException && cause.name === "AbortError")) {
        setError(cause instanceof Error ? cause.message : "Unknown EFCS error");
      }
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, [endpoint, pageSize, prsCode, requestJson]);

  useEffect(() => {
    if (!enabled) return;
    const controller = new AbortController();
    void reload(controller.signal);
    return () => controller.abort();
  }, [enabled, reload]);

  const loadMore = useCallback(() => {
    setVisibleCount((count) => Math.min(count + pageSize, rows.length));
  }, [pageSize, rows.length]);

  return {
    response,
    rows: visibleRows,
    total: rows.length,
    loading,
    error,
    hasMore,
    loadMore,
    reload: () => reload(),
  };
}
