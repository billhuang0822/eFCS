package com.efcs.mock.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MockMessageService {
    private static final Set<String> CODES = Set.of("T101", "T102", "T112", "T201");
    private static final DateTimeFormatter TXN_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public Map<String, Object> respond(String prsCode, Map<String, Object> request) {
        if (!CODES.contains(prsCode)) {
            throw new IllegalArgumentException("Unsupported prsCode: " + prsCode);
        }
        Map<?, ?> requestHead = asMap(request.get("HEAD"), "HEAD must be a JSON object");
        if (!prsCode.equals(requestHead.get("prsCode"))) {
            throw new IllegalArgumentException("URL prsCode must match HEAD.prsCode");
        }

        Map<String, Object> head = new LinkedHashMap<>();
        head.put("txnChannel", valueOr(requestHead.get("txnChannel"), "WEB"));
        head.put("prsCode", prsCode);
        head.put("txnDataTime", LocalDateTime.now().format(TXN_TIME));
        head.put("txnNo", valueOr(requestHead.get("txnNo"), "MOCK-" + System.currentTimeMillis()));
        head.put("billerId", valueOr(requestHead.get("billerId"), "MOCK-BILLER"));
        head.put("bTypeClass", valueOr(requestHead.get("bTypeClass"), ""));
        head.put("icChkCode", "0000");
        head.put("icChkDesc", "成功");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("HEAD", head);
        switch (prsCode) {
            case "T101" -> response.put("BODY", t101Body());
            case "T102" -> response.put("BODY", t102Body());
            case "T112" -> { /* The supplied T112 response schema contains HEAD only. */ }
            case "T201" -> response.put("BODY", Map.of("totalAmount", 100, "totalFee", 0));
            default -> throw new IllegalArgumentException("Unsupported prsCode: " + prsCode);
        }
        return response;
    }

    private Map<String, Object> t101Body() {
        Map<String, Object> queryHead = new LinkedHashMap<>();
        queryHead.put("pendingNo", "MOCK-PENDING-001");
        queryHead.put("pendingNotAfter", "20261231235959");
        queryHead.put("totalCount", 1);
        queryHead.put("totalAmount", 100);
        queryHead.put("billerNickName", "Mock Biller");
        queryHead.put("bTypeClassName", "Mock bill type");
        queryHead.put("billType", "01");
        queryHead.put("wFlag", "N");
        queryHead.put("mFlag", "N");
        queryHead.put("returnUrl", "");
        queryHead.put("querySeqNo", "MOCK-QUERY-001");

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("queryDetailNo", "1");
        detail.put("queryIsPay", "N");
        detail.put("queryBillData", "MOCK-BILL-001");
        detail.put("queryBillBatch", "001");
        detail.put("queryAmount", 100);
        detail.put("queryHC1", 0);
        detail.put("queryDate", "20261113");
        detail.put("queryDataNo", 1);
        for (int i = 1; i <= 10; i++) {
            detail.put("queryDispName" + i, i == 1 ? "Mock item" : "");
            detail.put("queryDispData" + i, i == 1 ? "Sample" : "");
        }
        return Map.of("QUERYHEAD", queryHead, "QUERYDETAIL", List.of(detail), "key", "MOCK-KEY");
    }

    private Map<String, Object> t102Body() {
        Map<String, Object> payHead = new LinkedHashMap<>();
        payHead.put("totalCount", 1);
        payHead.put("totalAmount", 100);
        payHead.put("tPaidCount", 1);
        payHead.put("tPaidAmount", 100);
        payHead.put("mFlag", "N");

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("detailNo", "1");
        detail.put("txnAmount", 100);
        detail.put("billData", "MOCK-BILL-001");
        detail.put("billBatch", "001");
        detail.put("rtnCode", "0000");
        detail.put("rtnCodeDesc", "成功");
        detail.put("efcsSeqNo", "MOCK-EFCS-0001");
        detail.put("efcsTxnDatetime", LocalDateTime.now().format(TXN_TIME));
        detail.put("billDate", "20261113");
        detail.put("payDataNo", 1);
        for (int i = 1; i <= 5; i++) {
            detail.put("writeOffName" + i, i == 1 ? "Mock receipt" : "");
            detail.put("writeOffData" + i, i == 1 ? "MOCK-RECEIPT-001" : "");
        }
        return Map.of("PAYHEAD", payHead, "PAYDETAIL", List.of(detail), "key", "MOCK-KEY");
    }

    private Map<?, ?> asMap(Object value, String message) {
        if (value instanceof Map<?, ?> map) return map;
        throw new IllegalArgumentException(message);
    }

    private Object valueOr(Object value, Object fallback) {
        return value == null ? fallback : value;
    }
}
