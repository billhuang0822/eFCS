package com.efcs.pay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Typed DTOs matching the transaction JSON files under Doc/. */
public final class MessageDtos {
    private MessageDtos() { }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Envelope<T>(@JsonProperty("HEAD") Head head, @JsonProperty("BODY") T body) { }
    public record Head(String txnChannel, String prsCode, String txnDataTime, String txnNo,
                       String billerId, String bTypeClass, String icChkCode, String icChkDesc) { }
    public record T101RequestBody(String queryType, String queryData1, String queryData2,
                                  String queryData3, String queryData4, String queryData5,
                                  String eBillSn, String outBankId, String key) { }
    public record T101ResponseBody(@JsonProperty("QUERYHEAD") T101QueryHead queryHead,
                                   @JsonProperty("QUERYDETAIL") List<T101QueryDetail> queryDetails, String key) { }
    public record T101QueryHead(String pendingNo, String pendingNotAfter, Integer totalCount,
                                Integer totalAmount, String billerNickName, String bTypeClassName,
                                String billType, String wFlag, String mFlag, String returnUrl, String querySeqNo) { }
    public record T101QueryDetail(String queryDetailNo, String queryIsPay, String queryBillData,
                                  String queryBillBatch, Integer queryAmount, Integer queryHC1,
                                  String queryDate, Integer queryDataNo,
                                  String queryDispName1, String queryDispData1, String queryDispName2,
                                  String queryDispData2, String queryDispName3, String queryDispData3,
                                  String queryDispName4, String queryDispData4, String queryDispName5,
                                  String queryDispData5, String queryDispName6, String queryDispData6,
                                  String queryDispName7, String queryDispData7, String queryDispName8,
                                  String queryDispData8, String queryDispName9, String queryDispData9,
                                  String queryDispName10, String queryDispData10) { }
    public record T102RequestBody(@JsonProperty("PAYHEAD") T102PayHead payHead,
                                  @JsonProperty("PAYDETAIL") List<PaymentDetail> payDetails, String key) { }
    public record T102PayHead(String pendingNo, String pendingNotAfter, Integer totalCount,
                              Integer totalAmount, Integer totalFee, String outBankId, String memberId,
                              String payDatetime, String checkType, String billType, String deviceId,
                              String carrierType, String carrierNo, String mobile, String accountNo,
                              String cardNo, String merchantId, String mccCode, String terminal,
                              String email, String querySeqNo) { }
    public record T102ResponseBody(@JsonProperty("PAYHEAD") T102ResponsePayHead payHead,
                                   @JsonProperty("PAYDETAIL") List<T102ResponseDetail> payDetails, String key) { }
    public record T102ResponsePayHead(Integer totalCount, Integer totalAmount, Integer tPaidCount,
                                      Integer tPaidAmount, String mFlag) { }
    public record T102ResponseDetail(String detailNo, Integer txnAmount, String billData, String billBatch,
                                     String rtnCode, String rtnCodeDesc, String efcsSeqNo,
                                     String efcsTxnDatetime, String billDate, Integer payDataNo,
                                     String writeOffName1, String writeOffData1, String writeOffName2,
                                     String writeOffData2, String writeOffName3, String writeOffData3,
                                     String writeOffName4, String writeOffData4, String writeOffName5,
                                     String writeOffData5) { }
    public record T112RequestBody(@JsonProperty("PAYHEAD") T112PayHead payHead,
                                  @JsonProperty("PAYDETAIL") List<PaymentDetail> payDetails) { }
    public record T112PayHead(String pendingNo, String pendingNotAfter, Integer totalCount,
                              Integer totalAmount, String outBankId, String memberId, String outAccountNum,
                              String payDatetime, String checkType, String billType, String deviceId,
                              String carrierType, String carrierNo, String mobile) { }
    public record T201RequestBody(@JsonProperty("PAYHEAD") T201PayHead payHead,
                                  @JsonProperty("PAYDETAIL") List<T201PayDetail> payDetails) { }
    public record T201PayHead(String pendingNo, String mFlag) { }
    public record T201PayDetail(String detailNo) { }
    public record T201ResponseBody(Integer totalAmount, Integer totalFee) { }
    public record PaymentDetail(String detailNo, Integer txnAmount, String billData, String billBatch) { }
}
