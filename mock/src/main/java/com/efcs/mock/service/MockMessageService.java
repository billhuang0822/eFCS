package com.efcs.mock.service;

import com.efcs.pay.dto.MessageDtos.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MockMessageService {
    private static final DateTimeFormatter TXN_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String T101_NEXT = "MOCK-T101-PAGE-2";
    private static final String T102_NEXT = "MOCK-T102-PAGE-2";
    private static final String END_KEY = "END";
    private final ObjectMapper objectMapper;

    public MockMessageService(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    public Envelope<?> respond(String prsCode, JsonNode input) {
        Class<?> requestType = switch (prsCode) {
            case "T101" -> T101RequestBody.class;
            case "T102" -> T102RequestBody.class;
            case "T112" -> T112RequestBody.class;
            case "T201" -> T201RequestBody.class;
            default -> throw new IllegalArgumentException("Unsupported prsCode: " + prsCode);
        };
        Envelope<?> request = objectMapper.convertValue(input, objectMapper.getTypeFactory()
                .constructParametricType(Envelope.class, requestType));
        if (request.head() == null || !prsCode.equals(request.head().prsCode())) {
            throw new IllegalArgumentException("URL prsCode must match HEAD.prsCode");
        }
        Head h = request.head();
        Head responseHead = new Head(valueOr(h.txnChannel(), "WEB"), prsCode,
                LocalDateTime.now().format(TXN_TIME), valueOr(h.txnNo(), "MOCK-" + System.currentTimeMillis()),
                valueOr(h.billerId(), "MOCK-BILLER"), valueOr(h.bTypeClass(), ""), "0000", "成功");
        return switch (prsCode) {
            case "T101" -> new Envelope<>(responseHead, t101Response((T101RequestBody) request.body()));
            case "T102" -> new Envelope<>(responseHead, t102Response((T102RequestBody) request.body()));
            case "T112" -> new Envelope<>(responseHead, null);
            case "T201" -> new Envelope<>(responseHead, new T201ResponseBody(100, 0));
            default -> throw new IllegalArgumentException("Unsupported prsCode: " + prsCode);
        };
    }

    private T101ResponseBody t101Response(T101RequestBody request) {
        String key = request == null || request.key() == null ? "" : request.key();
        T101QueryHead queryHead = new T101QueryHead("MOCK-PENDING-001", "20261231235959", 2, 300,
                "Mock Biller", "Mock bill type", "01", "N", "N", "", "MOCK-QUERY-001");
        if (key.isEmpty()) return new T101ResponseBody(queryHead, List.of(t101Detail("1", 100)), T101_NEXT);
        if (T101_NEXT.equals(key)) return new T101ResponseBody(queryHead, List.of(t101Detail("2", 200)), END_KEY);
        throw new IllegalArgumentException("Unknown T101 continuation key");
    }

    private T101QueryDetail t101Detail(String no, int amount) {
        String bill = "MOCK-BILL-" + no;
        return new T101QueryDetail(no, "N", bill, "001", amount, 0, "20261113", Integer.valueOf(no),
                "Mock item " + no, "Sample " + no, "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "");
    }

    private T102ResponseBody t102Response(T102RequestBody request) {
        String key = request == null || request.key() == null ? "" : request.key();
        T102ResponsePayHead payHead = new T102ResponsePayHead(2, 300, 2, 300, "N");
        if (key.isEmpty()) return new T102ResponseBody(payHead, List.of(t102Detail("1", 100)), T102_NEXT);
        if (T102_NEXT.equals(key)) return new T102ResponseBody(payHead, List.of(t102Detail("2", 200)), END_KEY);
        throw new IllegalArgumentException("Unknown T102 continuation key");
    }

    private T102ResponseDetail t102Detail(String no, int amount) {
        return new T102ResponseDetail(no, amount, "MOCK-BILL-" + no, "001", "0000", "成功",
                "MOCK-EFCS-" + no, LocalDateTime.now().format(TXN_TIME), "20261113", Integer.valueOf(no),
                "Mock receipt", "MOCK-RECEIPT-" + no, "", "", "", "", "", "", "", "");
    }

    private static String valueOr(String value, String fallback) { return value == null ? fallback : value; }
}
