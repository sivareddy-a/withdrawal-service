package com.siva.services.withdrawservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = UserRequest.UserRequestBuilder.class)
public class UserRequest {
    @JsonProperty("name")
    private final String name;
    @JsonProperty("balance")
    private final Double balance;
}