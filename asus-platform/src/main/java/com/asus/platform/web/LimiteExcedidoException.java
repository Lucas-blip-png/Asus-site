package com.asus.platform.web;

/** Lancada quando um limite do plano e atingido. Mapeada para HTTP 403. */
public class LimiteExcedidoException extends RuntimeException {
    public LimiteExcedidoException(String mensagem) {
        super(mensagem);
    }
}
