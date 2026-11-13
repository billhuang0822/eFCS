package com.efcs.utility.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Wire envelope DTOs. Jackson 3 databind types use tools.jackson.*; annotations retain this package. */
public final class EfcsMessages {
    private EfcsMessages() { }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Envelope<T>(@JsonProperty("HEAD") Head head, @JsonProperty("BODY") T body) { }

    public record Head(String txnChannel, String prsCode, String txnDataTime, String txnNo,
                       String billerId, String bTypeClass, String icChkCode, String icChkDesc) { }

    public record T101Request(String queryType, String queryData1, String queryData2,
                              String queryData3, String queryData4, String queryData5,
                              String eBillSn, String outBankId, String key) { }

    public record T102Request(
            @JsonProperty("PAYHEAD") T102PayHead payHead,
            @JsonProperty("PAYDETAIL") java.util.List<PaymentDetail> payDetails,
            String key) { }

    public record T102PayHead(String pendingNo, String pendingNotAfter, Integer totalCount,
                              Integer totalAmount, Integer totalFee, String outBankId,
                              String memberId, String payDatetime, String checkType,
                              String billType, String deviceId, String carrierType,
                              String carrierNo, String mobile, String accountNo, String cardNo,
                              String merchantId, String mccCode, String terminal, String email,
                              String querySeqNo) { }

    public record PaymentDetail(String detailNo, Integer txnAmount, String billData, String billBatch) { }
}
