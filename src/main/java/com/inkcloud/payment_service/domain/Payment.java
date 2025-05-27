package com.inkcloud.payment_service.domain;

import java.time.LocalDateTime;


import com.inkcloud.payment_service.enums.PaymentMethod;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payment {
    @Id
    @Column(name = "payment_id")
    private String paymentId;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    private int price;
    private int count;
    
    private LocalDateTime at;       // 결제일자

    private String pg;              // pg사 정보
    @Column(name = "tx_id")
    private String txId;             // 거래번호
    @Column(name = "appl_num")
    private String applNum;         // 승인번호

}
