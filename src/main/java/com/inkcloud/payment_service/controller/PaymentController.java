package com.inkcloud.payment_service.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inkcloud.payment_service.dto.PaymentDto;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;




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
    
    // @PostMapping("/validate")
    // public ResponseEntity<PaymentValidateDto> postValidationAddServer(@RequestBody PaymentValidateDto paymentValidateDto) {
    //     PaymentValidateDto dto = paymentService.addPayValidationData(paymentValidateDto);
    //     return new ResponseEntity<>(dto, HttpStatus.OK);
    // }

    // <OrderId, OrderId>
    @PutMapping
    public ResponseEntity<String> cancelPayment(@RequestBody Map<String,String> req) {
        return new ResponseEntity<>(paymentService.cancelPay(req.get("order_id")).toString(),HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<PaymentDto> getPaymentInfo(@RequestParam(value = "order_id") String orderId) {
        return new ResponseEntity<>(paymentService.retreivePayment(orderId), HttpStatus.OK);
    }
    
    
    
}
