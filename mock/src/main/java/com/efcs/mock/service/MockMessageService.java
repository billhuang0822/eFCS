package com.efcs.mock.service;

import com.efcs.pay.dto.MessageDtos;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.efcs.pay.dto.MessageDtos.*;

@Service
public class MockMessageService {
    private static final DateTimeFormatter TXN_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
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
            case "T101" -> new Envelope<>(responseHead, t101Response());
            case "T102" -> new Envelope<>(responseHead, t102Response());
            case "T112" -> new Envelope<>(responseHead, null);
            case "T201" -> new Envelope<>(responseHead, new T201ResponseBody(100, 0));
            default -> throw new IllegalArgumentException("Unsupported prsCode: " + prsCode);
        };
    }

    private T101ResponseBody t101Response() {
        T101QueryHead queryHead = new T101QueryHead("MOCK-PENDING-001", "20261231235959", 1, 100,
                "Mock Biller", "Mock bill type", "01", "N", "N", "", "MOCK-QUERY-001");
        T101QueryDetail detail = new T101QueryDetail("1", "N", "MOCK-BILL-001", "001", 100, 0,
                "20261113", 1, "Mock item", "Sample", "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "");
        return new T101ResponseBody(queryHead, List.of(detail), "MOCK-KEY");
    }

    private T102ResponseBody t102Response() {
        T102ResponsePayHead payHead = new T102ResponsePayHead(1, 100, 1, 100, "N");
        T102ResponseDetail detail = new T102ResponseDetail("1", 100, "MOCK-BILL-001", "001", "0000",
                "成功", "MOCK-EFCS-0001", LocalDateTime.now().format(TXN_TIME), "20261113", 1,
                "Mock receipt", "MOCK-RECEIPT-001", "", "", "", "", "", "", "", "");
        return new T102ResponseBody(payHead, List.of(detail), "MOCK-KEY");
    }

    private static String valueOr(String value, String fallback) { return value == null ? fallback : value; }
}
