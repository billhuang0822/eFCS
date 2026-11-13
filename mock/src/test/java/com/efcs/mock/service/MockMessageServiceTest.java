package com.efcs.mock.service;

import com.efcs.pay.dto.MessageDtos.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MockMessageServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final MockMessageService service = new MockMessageService(mapper);

    @Test
    void t101ReturnsNextKeyThenEnd() {
        JsonNode first = mapper.valueToTree(service.respond("T101", mapper.valueToTree(
                new Envelope<>(head("T101"), new T101RequestBody("01", "A001", "", "", "", "", "", "", "")))));
        assertEquals("MOCK-T101-PAGE-2", first.path("BODY").path("key").asText());
        assertEquals(1, first.path("BODY").path("QUERYDETAIL").size());

        JsonNode secondRequest = requestWithKey("T101", new T101RequestBody("01", "A001", "", "", "", "", "", "", ""),
                first.path("BODY").path("key").asText());
        JsonNode second = mapper.valueToTree(service.respond("T101", secondRequest));
        assertEquals("END", second.path("BODY").path("key").asText());
        assertEquals("2", second.path("BODY").path("QUERYDETAIL").get(0).path("queryDetailNo").asText());
    }

    @Test
    void t102ReturnsNextKeyThenEnd() {
        T102PayHead payHead = new T102PayHead("PENDING-1", "", 1, 100, 0, "", "M1", "", "", "01",
                "", "", "", "", "", "", "", "", "", "", "Q1");
        T102RequestBody body = new T102RequestBody(payHead,
                List.of(new PaymentDetail("1", 100, "BILL-1", "001")), "");
        JsonNode first = mapper.valueToTree(service.respond("T102", mapper.valueToTree(new Envelope<>(head("T102"), body))));
        assertEquals("MOCK-T102-PAGE-2", first.path("BODY").path("key").asText());
        JsonNode second = mapper.valueToTree(service.respond("T102", requestWithKey("T102", body,
                first.path("BODY").path("key").asText())));
        assertEquals("END", second.path("BODY").path("key").asText());
        assertEquals("2", second.path("BODY").path("PAYDETAIL").get(0).path("detailNo").asText());
    }

    @Test
    void rejectsUnknownCursorAndMismatchedCode() {
        T101RequestBody body = new T101RequestBody("01", "A001", "", "", "", "", "", "", "UNKNOWN");
        assertThrows(IllegalArgumentException.class,
                () -> service.respond("T101", requestWithKey("T101", body, "UNKNOWN")));
        assertThrows(IllegalArgumentException.class,
                () -> service.respond("T201", mapper.valueToTree(new Envelope<>(head("T101"), body))));
    }

    private JsonNode requestWithKey(String code, Object body, String key) {
        JsonNode json = mapper.valueToTree(new Envelope<>(head(code), body));
        ((com.fasterxml.jackson.databind.node.ObjectNode) json.path("BODY")).put("key", key);
        return json;
    }

    private Head head(String code) {
        return new Head("WEB", code, "20261113120000", "test-1", "BILLER001", "", "", "");
    }
}
