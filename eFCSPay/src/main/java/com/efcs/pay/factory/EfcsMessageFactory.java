package com.efcs.pay.factory;

import com.efcs.pay.dto.MessageDtos.*;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/** Creates typed envelopes using the HEAD layout from the supplied eFCS JSON samples. */
public final class EfcsMessageFactory {
    private static final DateTimeFormatter TXN_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final Clock clock;

    public EfcsMessageFactory(Clock clock) { this.clock = clock; }
    public EfcsMessageFactory() { this(Clock.systemDefaultZone()); }

    public record HeadContext(String txnChannel, String billerId, String bTypeClass) { }

    public Envelope<T101RequestBody> t101(HeadContext context, T101RequestBody body) {
        return envelope(context, "T101", body);
    }

    public Envelope<T102RequestBody> t102(HeadContext context, T102RequestBody body) {
        return envelope(context, "T102", body);
    }

    public Envelope<T112RequestBody> t112(HeadContext context, T112RequestBody body) {
        return envelope(context, "T112", body);
    }

    public Envelope<T201RequestBody> t201(HeadContext context, T201RequestBody body) {
        return envelope(context, "T201", body);
    }

    private <T> Envelope<T> envelope(HeadContext context, String code, T body) {
        if (context == null) throw new IllegalArgumentException("HeadContext is required");
        if (body == null) throw new IllegalArgumentException("BODY is required for " + code);
        Head head = new Head(defaultValue(context.txnChannel(), "WEB"), code,
                LocalDateTime.now(clock).format(TXN_TIME), UUID.randomUUID().toString(),
                defaultValue(context.billerId(), ""), defaultValue(context.bTypeClass(), ""), "", "");
        return new Envelope<>(head, body);
    }

    public static PaymentDetail paymentDetail(String detailNo, int amount, String billData, String billBatch) {
        return new PaymentDetail(detailNo, amount, billData, billBatch);
    }

    public static T201PayDetail cancelDetail(String detailNo) {
        return new T201PayDetail(detailNo);
    }

    private static String defaultValue(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
