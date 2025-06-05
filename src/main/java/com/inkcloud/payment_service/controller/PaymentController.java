package com.inkcloud.payment_service.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inkcloud.payment_service.dto.PaymentValidateDto;
import com.inkcloud.payment_service.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PutMapping;



@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/webhook")
    public ResponseEntity<String> webHookRecv(
                            @RequestBody String payload,
                            @RequestHeader("webhook-id") String webhookId,
                            @RequestHeader("webhook-signature") String webhookSignagure,
                            @RequestHeader("webhook-timestamp") String webhookTimestamp
                            ) {
                            
        paymentService.processWebhook(webhookId, webhookSignagure, webhookTimestamp, payload);
        
        return new ResponseEntity<>(payload, HttpStatus.OK);
    }
    
    @PostMapping("/validate")
    public ResponseEntity<PaymentValidateDto> postValidationAddServer(@RequestBody PaymentValidateDto paymentValidateDto) {
        PaymentValidateDto dto = paymentService.addPayValidationData(paymentValidateDto);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    // 주문 서비스 제작시 비동기 처리 고려 --> 관리자 기능때문에 남겨놔야함
    @PutMapping
    public ResponseEntity<String> cancelPayment(@RequestBody Map<String,String> req) {
        paymentService.cancelPay(req.get("payment_id"));
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    
}
