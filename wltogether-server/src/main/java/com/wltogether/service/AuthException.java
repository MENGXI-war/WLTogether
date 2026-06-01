package com.wltogether.service;

import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {
    private final String error;

    public AuthException(String error, String message) {
        super(message);
        this.error = error;
    }
}
