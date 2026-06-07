package com.asus.platform.web;

/** Credenciais invalidas ou ausentes. Mapeada para HTTP 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String mensagem) {
        super(mensagem);
    }
}
