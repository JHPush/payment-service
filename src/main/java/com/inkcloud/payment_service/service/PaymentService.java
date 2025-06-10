package com.inkcloud.payment_service.service;


import com.inkcloud.payment_service.domain.Payment;
import com.inkcloud.payment_service.dto.PaymentDto;
import com.inkcloud.payment_service.dto.WebhookPayload;
import com.inkcloud.payment_service.enums.PaymentStatus;


public interface PaymentService {
    /// 웹훅 처리 메인 메소드
    abstract void processWebhook(String webhookId, String webhookSignature,
                                    String webhookTimestamp, String payload);
    /// 타임스탬프 검증 (5분 유지)
    abstract void verifyTimeStamp(String timestamp);
    /// 요청 데이터 기반 시그니처 생성 -> sdk 로 검증변경
    // abstract String generateSignature(String webhookId,String webhookTimestamp, String payload);
    
    /// 시그니쳐 비교 
    /// 생성된 시그니처와 포트원 제공 시그니처가 동일한지 확인 -> sdk 검증으로 변경
    // abstract boolean verfiySignature(String exceptedSignature, String actualSignature);
    
    /// 웹훅 데이터 json 파싱
    abstract WebhookPayload parsePayload(String payload);

    // 웹훅 이벤트 핸들링
    abstract void handleEvent(WebhookPayload data);

    // 결제창 오픈시 검증 데이터 저장 -> 결제 완료시 삭제
    // abstract PaymentValidateDto addPayValidationData(PaymentValidateDto paymentId);

    abstract PaymentStatus cancelPay(String orderId);
    abstract PaymentDto retreivePayment(String orderId);

    default PaymentDto entityToDto(Payment payment){
        return PaymentDto.builder()
                                .method(payment.getMethod())
                                .price(payment.getPrice())
                                .count(payment.getCount())
                                .at(payment.getAt())
                                .pg(payment.getPg())
                                .build();
    }

}
