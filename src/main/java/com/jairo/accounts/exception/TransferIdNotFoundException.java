package com.jairo.accounts.exception;

public class TransferIdNotFoundException extends RuntimeException{

    public TransferIdNotFoundException(String message) {
        super(message);
    }
}
