package com.siva.services.withdrawservice.model;

import lombok.Getter;

@Getter
public enum TransactionStatus {
    PROCESSING("processing"),
    COMPLETED("completed"),
    FAILED("failed");

    private final String status;

    TransactionStatus(String status) {
        this.status = status;
    }

    public static TransactionStatus fromString(String st) {
        return TransactionStatus.valueOf(st.toUpperCase());
    }

    @Override
    public String toString() {
        return this.getStatus();
    }
}
