package com.efcs.mock.service;

import com.efcs.pay.dto.MessageDtos.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MockMessageServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final MockMessageService service = new MockMessageService(mapper);

    @Test
    void returnsTypedT101ResponseWithWirePropertyNames() throws Exception {
        var request = new Envelope<>(head("T101"),
                new T101RequestBody("01", "A001", "", "", "", "", "", "", ""));
        var response = service.respond("T101", mapper.valueToTree(request));
        assertEquals("0000", response.head().icChkCode());
        assertInstanceOf(T101ResponseBody.class, response.body());
        var json = mapper.valueToTree(response);
        assertTrue(json.path("BODY").has("QUERYHEAD"));
        assertTrue(json.path("BODY").has("QUERYDETAIL"));
        assertEquals("T101", json.path("HEAD").path("prsCode").asText());
    }

    @Test
    void returnsT102PaymentDetails() {
        var payHead = new T102PayHead("PENDING-1", "", 1, 100, 0, "", "M1", "", "", "01",
                "", "", "", "", "", "", "", "", "", "", "Q1");
        var request = new Envelope<>(head("T102"),
                new T102RequestBody(payHead, List.of(new PaymentDetail("1", 100, "BILL-1", "001")), ""));
        var response = service.respond("T102", mapper.valueToTree(request));
        var body = assertInstanceOf(T102ResponseBody.class, response.body());
        assertEquals("0000", body.payDetails().getFirst().rtnCode());
    }

    @Test
    void t112HasHeadOnlyAndRejectsMismatchedCode() {
        var request = new Envelope<>(head("T112"), new T112RequestBody(null, List.of()));
        var response = service.respond("T112", mapper.valueToTree(request));
        assertNull(response.body());
        assertThrows(IllegalArgumentException.class,
                () -> service.respond("T201", mapper.valueToTree(request)));
    }

    private Head head(String code) {
        return new Head("WEB", code, "20261113120000", "test-1", "BILLER001", "", "", "");
    }
}
