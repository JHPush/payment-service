package com.inkcloud.payment_service.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inkcloud.payment_service.dto.PaymentValidateDto;
import com.inkcloud.payment_service.enums.PaymentStatus;
import com.inkcloud.payment_service.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;


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
    
    // 최종 결제 내역 확인
    @PostMapping("/validate")
    public ResponseEntity<String> paymentValidation(@RequestBody PaymentValidateDto paymentValidateDto) {
        PaymentStatus status = paymentService.payValidation(paymentValidateDto);
        return new ResponseEntity<>(status.toString(), HttpStatus.OK);
    }
    
    
}
