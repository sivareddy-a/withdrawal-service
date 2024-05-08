package com.siva.services.withdrawservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TransferRequest.TransferRequestBuilder.class)
public class TransferRequest {
    @JsonProperty("sender_id")
    private final UUID senderId;
    //receiver_id is address_id of 3rd party wallet
    @JsonProperty("receiver_id")
    private final UUID receiverId;
    @JsonProperty("amount")
    private final Double amount;
}
