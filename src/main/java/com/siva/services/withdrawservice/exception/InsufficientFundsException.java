package com.siva.services.withdrawservice.exception;

public class InsufficientFundsException extends Exception{
    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(){
        this("Insufficient funds!");
    }
}
