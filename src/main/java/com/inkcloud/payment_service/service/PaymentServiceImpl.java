package com.inkcloud.payment_service.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkcloud.payment_service.domain.Payment;
import com.inkcloud.payment_service.dto.PaymentValidateDto;
import com.inkcloud.payment_service.dto.WebhookPayload;
import com.inkcloud.payment_service.enums.PaymentMethod;
import com.inkcloud.payment_service.enums.PaymentStatus;
import com.inkcloud.payment_service.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository repo;
    private final WebClient webClient;

    @Value("${SPRING_WEBHOOK_SECRET}")
    private String WEBHOOK_SECRET;

    @Override
    public void processWebhook(String webhookId, String webhookSignature, String webhookTimestamp, String payload) {
        verifyTimeStamp(webhookTimestamp); // 타임스탬프 검증
        String exceptedSignature = generateSignature(webhookId, webhookTimestamp, payload);

        if (!verfiySignature(exceptedSignature, exceptedSignature))
            throw new IllegalArgumentException("유효하지 않은 시그니처");

        WebhookPayload data = parsePayload(payload);
        handleEvent(data);
    }

    @Override
    public String generateSignature(String webhookId, String webhookTimestamp, String payload) {
        try {
            String dataToSign = String.join(".", webhookId, webhookTimestamp, payload); // 데이터 조합
            Mac mac = Mac.getInstance("HmacSHA256"); // HMAC-SHA256 알고리즘 사용
            mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(), "HmacSHA256")); // 시크릿 키 설정
            return Base64.getEncoder().encodeToString(mac.doFinal(dataToSign.getBytes())); // 시그니처 생성
        } catch (Exception e) {
            throw new RuntimeException("시그니처 생성 중 오류 발생", e);
        }
    }

    @Override
    public void handleEvent(WebhookPayload data) {
        String type = data.getStatus();
        String paymentId = data.getPayment_id();

        switch (type) {
            case "Ready":
                log.info("결제창 오픈 처리");
                break;
            case "Paid":
                log.info("결제 완료 이벤트 처리");
                /// TODO : 결제 금액 검증 & OrderState 변경 & SSE 전송
                break;
            case "Cancelled":
                log.info("결제 취소 이벤트 처리");
                break;

            default:
                log.info("알수 없는 이벤트 : {}", type);
                break;
        }
    }

    @Override
    public WebhookPayload parsePayload(String payload) {
        try {
            log.info("payload : {}", payload);
            return new ObjectMapper().readValue(payload, WebhookPayload.class); // 잭슨
        } catch (Exception e) {
            throw new RuntimeException("Json 파싱 오류 : ", e);
        }
    }

    @Override
    public boolean verfiySignature(String exceptedSignature, String actualSignature) {
        return exceptedSignature.equals(actualSignature);
    }

    @Override
    public void verifyTimeStamp(String timestamp) {
        long now = System.currentTimeMillis() / 1000;
        long requestTimestamp = Long.parseLong(timestamp);
        if (Math.abs(now - requestTimestamp) > 300)
            throw new IllegalArgumentException("타임스탬프 만료");
    }

    @Override
    public PaymentStatus payValidation(PaymentValidateDto dto) {
        JsonNode response = webClient.get()
                .uri("/" + dto.getPaymentId())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        if (response == null || !response.get("status").asText().equals("PAID") ) {
            log.error("PortOne response is null or Payment Failed : {}", dto.getPaymentId());
            return PaymentStatus.FAILED;
        }
        PaymentValidateDto compareDto = extractPaymentData(response);
            log.info("dt entity {} ", compareDto);
            log.info("dt2 entity {} ", dto);
        
        if(compareDto.equals(dto)){
            Instant instant = Instant.parse(response.get("paidAt").asText());
            LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
            PaymentMethod method = convertStringToPaymentMethod(response.get("method").get("type").asText());
        
            Payment payment = Payment.builder()
                                            .paymentId(dto.getPaymentId())
                                            .method(method)
                                            .price(compareDto.getTotalAmount())
                                            .count(compareDto.getTotalCount())
                                            .price(compareDto.getTotalAmount())
                                            .at(dateTime)
                                            .pg(response.get("channel").get("pgProvider").asText())
                                            .txId(response.get("transactionId").asText())
                                            .applNum(response.get("method").get("approvalNumber").asText())
                                            .build();
            repo.save(payment);
            log.info("save entity");
            return PaymentStatus.PAID;
        }
            log.info("failed entity");

        return PaymentStatus.FAILED;

        // response.subscribe(
        // data-> {
        // log.info("id : {}", data.get("id").asText());
        // log.info("email : {} ", data.get("customer").get("email").asText());
        // log.info("count : {}", data.get("products").get(0).get("quantity").asLong());
        // log.info("amount : {}", data.get("amount").get("total").asLong());
        // },
        // error-> log.error("paymentInfo Error : {}", error.getMessage())
        // );
        // response.doOnSuccess(data->{
        // log.info("on Success : {} ", data);
        // });

        // return PaymentStatus.PAID;
    }

    private PaymentValidateDto extractPaymentData(JsonNode response) {
        PaymentMethod method = convertStringToPaymentMethod(response.get("method").get("type").asText());
       
        String id = response.get("id").asText();
        String email = response.get("customer").get("email").asText();
        int quantity = response.get("products").get(0).get("quantity").asInt();
        int totalAmount = response.get("amount").get("total").asInt();

        return PaymentValidateDto.builder()
                .paymentId(id)
                .email(email)
                .method(method)
                .totalCount(quantity)
                .totalAmount(totalAmount)
                .build();
    }

    public PaymentMethod convertStringToPaymentMethod(String method){
        String conv[] = method.split("d",2);
        log.info("method : {}", conv[1]);
        switch (conv[1]) {
            case "Card":
                return PaymentMethod.CARD;
            case "EasyPay":
                return PaymentMethod.EASY_PAY;
            default:
                return PaymentMethod.CARD;
        }
    }

}
