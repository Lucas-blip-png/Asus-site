package com.asus.platform.web;

/** Excesso de tentativas (rate limit). Mapeada para HTTP 429. */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String mensagem) {
        super(mensagem);
    }
}
