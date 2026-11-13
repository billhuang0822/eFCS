package com.efcs.pay.factory;

import com.efcs.pay.dto.MessageDtos.*;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EfcsMessageFactoryTest {
    private final EfcsMessageFactory factory = new EfcsMessageFactory(
            Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZoneOffset.UTC));
    private final EfcsMessageFactory.HeadContext context =
            new EfcsMessageFactory.HeadContext("WEB", "BILLER001", "01");

    @Test
    void createsT101EnvelopeWithProtocolHeader() {
        var body = new T101RequestBody("01", "A001", "", "", "", "", "", "", "TEST-KEY");
        var message = factory.t101(context, body);
        assertEquals("T101", message.head().prsCode());
        assertEquals("20261113120000", message.head().txnDataTime());
        assertEquals("BILLER001", message.head().billerId());
        assertSame(body, message.body());
        assertNotNull(message.head().txnNo());
    }

    @Test
    void createsT102WithDetailList() {
        var payHead = new T102PayHead("PENDING-1", "", 1, 100, 0, "", "MEMBER-1", "", "",
                "01", "", "", "", "", "", "", "", "", "", "", "QUERY-1");
        var body = new T102RequestBody(payHead,
                List.of(EfcsMessageFactory.paymentDetail("1", 100, "BILL-1", "001")), "KEY");
        var message = factory.t102(context, body);
        assertEquals("T102", message.head().prsCode());
        assertEquals(1, message.body().payDetails().size());
        assertEquals(100, message.body().payDetails().getFirst().txnAmount());
    }

    @Test
    void rejectsMissingContextOrBody() {
        assertThrows(IllegalArgumentException.class, () -> factory.t101(null,
                new T101RequestBody("01", "", "", "", "", "", "", "", "")));
        assertThrows(IllegalArgumentException.class, () -> factory.t112(context, null));
    }
}
