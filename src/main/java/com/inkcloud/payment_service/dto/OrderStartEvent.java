package com.inkcloud.payment_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OrderStartEvent {
    private String id;
    private PaymentValidateDto order;
}
