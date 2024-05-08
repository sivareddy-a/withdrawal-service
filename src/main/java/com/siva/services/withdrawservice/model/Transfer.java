package com.siva.services.withdrawservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Transfer.TransferBuilder.class)
public class Transfer {
    @JsonProperty("id")
    private final UUID id;
    @JsonProperty("sender_id")
    private final UUID senderId;
    //receiver_id is user_id of receiver
    @JsonProperty("receiver_id")
    private final UUID receiverId;
    @JsonProperty("amount")
    private final Double amount;
    @JsonProperty("status")
    private final TransactionStatus status;
    @JsonProperty("failure")
    private final String failure;
    @JsonProperty("created_at")
    Instant createdAt;
    @JsonProperty("updated_at")
    Instant updatedAt;
}
