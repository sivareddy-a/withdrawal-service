package com.siva.services.withdrawservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WithdrawalRequest.WithdrawalRequestBuilder.class)
public class WithdrawalRequest {
    @JsonProperty("sender_id")
    private final UUID senderId;
    //receiver_id is address_id of 3rd party wallet
    @JsonProperty("address")
    private final String address;
    @JsonProperty("amount")
    private final Double amount;
}
