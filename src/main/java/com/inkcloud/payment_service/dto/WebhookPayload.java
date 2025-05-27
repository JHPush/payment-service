package com.inkcloud.payment_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPayload {
    @JsonProperty("tx_id")
    String tx_id;
    @JsonProperty("payment_id")
    String payment_id;
    @JsonProperty("status")
    String status;

}