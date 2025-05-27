package com.inkcloud.payment_service.dto;

import java.util.Objects;

import com.inkcloud.payment_service.enums.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PaymentValidateDto {
    String paymentId;
    String email;
    PaymentMethod method;
    int totalCount;
    int totalAmount;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PaymentValidateDto that = (PaymentValidateDto) o;
        return totalCount == that.totalCount &&
                totalAmount == that.totalAmount &&
                Objects.equals(paymentId, that.paymentId) &&
                Objects.equals(email, that.email) &&
                Objects.equals(method, that.method);
    }

}
