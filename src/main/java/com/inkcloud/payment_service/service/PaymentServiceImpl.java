package com.inkcloud.payment_service.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkcloud.payment_service.domain.Payment;
import com.inkcloud.payment_service.dto.PaymentEvent;
import com.inkcloud.payment_service.dto.PaymentValidateDto;
import com.inkcloud.payment_service.dto.WebhookPayload;
import com.inkcloud.payment_service.enums.PaymentMethod;
import com.inkcloud.payment_service.enums.PaymentStatus;
import com.inkcloud.payment_service.repository.PaymentRepository;

import io.portone.sdk.server.webhook.Webhook;
import io.portone.sdk.server.webhook.WebhookVerifier;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository repo;
    private final WebClient webClient;
    private Map<String, PaymentValidateDto> comparePaymentDatas = new HashMap<>();
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Value("${SPRING_WEBHOOK_SECRET}")
    private String WEBHOOK_SECRET;

    @Value("${SPRING_STORE_ID}")
    private String storeId;

    @Override
    public void processWebhook(String webhookId, String webhookSignature, String webhookTimestamp, String payload) {
        verifyTimeStamp(webhookTimestamp); // 타임스탬프 검증
        try {
            WebhookVerifier verifier = new WebhookVerifier(WEBHOOK_SECRET);
            Webhook webhook = verifier.verify(payload, webhookId, webhookSignature, webhookTimestamp);
            if (webhook == null) {
                log.error("웹훅 검증 실패");
                throw new RuntimeException("Not verifier Web Hook");
            }
            log.info("웹훅 검증 완료");
        } catch (Exception e) {
            log.error("유효하지 않은 시그니처", e);
        }

        // String exceptedSignature = generateSignature(webhookId, webhookTimestamp,
        // payload);
        // if (!verfiySignature(exceptedSignature, webhookSignature))
        // throw new IllegalArgumentException("유효하지 않은 시그니처");

        WebhookPayload data = parsePayload(payload);
        handleEvent(data);
    }

    // 직접 시그니처 검증 -> sdk 사용해서 검증
    // @Override
    // public String generateSignature(String webhookId, String webhookTimestamp,
    // String payload) {
    // try {
    // String toSign = String.format("%s.%s.%s", webhookId, webhookTimestamp,
    // payload);
    // Mac sha512Hmac = Mac.getInstance("HmacSHA256");
    // SecretKeySpec keySpec = new
    // SecretKeySpec(WEBHOOK_SECRET.split("_")[1].getBytes(StandardCharsets.UTF_8),
    // "HmacSHA256");
    // sha512Hmac.init(keySpec);
    // byte[] macData = sha512Hmac.doFinal(toSign.getBytes(StandardCharsets.UTF_8));
    // String signature = Base64.getEncoder().encodeToString(macData);
    // return String.format("v1,%s", signature);
    // } catch (Exception e) {
    // throw new RuntimeException("시그니처 생성 중 오류 발생", e);
    // }
    // }

    @Override
    public void handleEvent(WebhookPayload data) {
        String type = data.getStatus();
        String paymentId = data.getPayment_id();

        switch (type) {
            case "Ready":
                log.info("결제창 오픈 처리");
                break;
            case "Paid":
                try {
                    Mono<JsonNode> res = webClient.get()
                            .uri("/" + paymentId)
                            .retrieve()
                            .bodyToMono(JsonNode.class);
                    res.subscribe(response -> {
                        if (response == null || !response.get("status").asText().equals("PAID")) {
                            log.error("PortOne response is null or Payment Failed : {}", paymentId);
                            throw new IllegalArgumentException("결제 데이터 오류");
                            // 오류시 카프카 이벤트 발송 필요
                        }
                        PaymentValidateDto compareDto = extractPaymentData(response);
                        log.info("compare dto =================== {} ", compareDto);
                        if (compareDto.equals(comparePaymentDatas.get(paymentId))) {
                            Instant instant = Instant.parse(response.get("paidAt").asText());
                            LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Seoul"));
                            PaymentMethod method = convertStringToMethod(
                                    response.get("method").get("type").asText());
                            String applNum = null;
                            switch (method) {
                                case CARD:
                                    applNum = response.get("method").get("approvalNumber").asText();
                                    break;
                                case EASYPAY:
                                    method = convertStringToMethod(response.get("method").get("provider").asText());
                                    break;
                            }
                            Payment payment = Payment.builder()
                                    .paymentId(paymentId)
                                    .method(method)
                                    .price(compareDto.getPrice())
                                    .count(compareDto.getQuantity())
                                    .status(PaymentStatus.valueOf(response.get("status").asText()))
                                    .at(dateTime)
                                    .pg(response.get("channel").get("pgProvider").asText())
                                    .txId(response.get("transactionId").asText())
                                    .applNum(applNum)
                                    .orderId(comparePaymentDatas.get(paymentId).getOrderId())
                                    .build();
                            repo.save(payment);
                            log.info("save entity");

                            PaymentEvent event = new PaymentEvent();
                            event.setId(paymentId);
                            event.setOrder(comparePaymentDatas.get(paymentId));

                            kafkaTemplate.send("payment-success", event);
                            comparePaymentDatas.remove(paymentId);

                        } else
                            throw new IllegalArgumentException("결제 정보 불일치");
                    });
                } catch (Exception e) {
                    log.error("포트원 리스폰스 잭슨 파싱 확인 필요");
                    cancelPay(paymentId);
                    log.error("Entity Error in Payment-service : {}", e);
                }
                break;
            case "Cancelled":
                log.info("결제 취소 후 로직");
                Payment payment = repo.findById(paymentId).orElseThrow(() -> new IllegalArgumentException("결제 정보 불일치"));
                payment.setStatus(PaymentStatus.CANCELLED);
                // 여기에 결제 취소시 카프카 이벤트 발송 필요 할수도?

            case "Failed":
                if (comparePaymentDatas.containsKey(paymentId))
                    comparePaymentDatas.remove(paymentId);
                log.info("결제 실패 알림");
                break;
            default:
                log.info("알수 없는 이벤트 : {}", type);
                break;
        }
    }

    public PaymentMethod convertStringToMethod(String s) {
        return PaymentMethod.valueOf(s.replace("PaymentMethod", "").toUpperCase());
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

    // sdk 검증방식으로 변경
    // @Override
    // public boolean verfiySignature(String exceptedSignature, String
    // actualSignature) {
    // log.info("시그니처 웹훅 : {}", actualSignature);
    // log.info("시그니처 생성 : {}", exceptedSignature);
    // return exceptedSignature.equals(actualSignature);
    // }

    @Override
    public void verifyTimeStamp(String timestamp) {
        long now = System.currentTimeMillis() / 1000;
        long requestTimestamp = Long.parseLong(timestamp);
        if (Math.abs(now - requestTimestamp) > 300)
            throw new IllegalArgumentException("타임스탬프 만료");
    }

    @Override
    public PaymentValidateDto addPayValidationData(PaymentValidateDto dto) {
        comparePaymentDatas.put(dto.getPaymentId(), dto);
        return dto;
    }

    private PaymentValidateDto extractPaymentData(JsonNode response) {
        PaymentMethod method = convertStringToMethod(response.get("method").get("type").asText());

        String id = response.get("id").asText();
        String email = response.get("customer").get("email").asText();
        int quantity = IntStream.range(0, response.get("products").size())
                .map(i -> response.get("products").get(i).get("quantity").asInt()).sum();

        int totalAmount = response.get("amount").get("total").asInt();

        return PaymentValidateDto.builder()
                .paymentId(id)
                .email(email)
                // .method(method)
                .quantity(quantity)
                .price(totalAmount)
                .build();
    }

    @Override
    public PaymentStatus cancelPay(String orderId) {

        Payment p = repo.findByOrderId(orderId);

        Map<String, Object> cancelRequest = new HashMap<>();
        cancelRequest.put("storeId", storeId);
        cancelRequest.put("reason", "승인 취소");

        Mono<JsonNode> res = webClient.post()
                .uri("/" + p.getPaymentId() + "/cancel")
                .bodyValue(cancelRequest)
                .retrieve()
                .bodyToMono(JsonNode.class);
        res.subscribe(data -> {
            log.info("결제 취소 완료 : {}", data.get(p.getPaymentId()));
        }, error -> {
            log.error("Error In Pay Cancled : {} ", error.getMessage());
            throw new RuntimeException("In Payment Cancelled Error : ", error);
        });

        return PaymentStatus.CANCELLED;
    }

    @KafkaListener(topics = "order-verify", groupId = "order-group")
    public void paymentVerify(String event) throws Exception {
        log.info("kafka Consumer : Payment-service, receive event : {}", event);
        try {
            PaymentEvent ev = new ObjectMapper().readValue(event, PaymentEvent.class);
            log.info("kafka mapping success in Payment-Service : {}", event);
            comparePaymentDatas.put(ev.getOrder().getPaymentId(), ev.getOrder());
        } catch (Exception e) {
            throw e;
        }
    }
}
