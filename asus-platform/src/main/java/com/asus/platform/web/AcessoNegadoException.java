package com.asus.platform.web;

/** Usuario autenticado, porem sem permissao sobre o recurso (HTTP 403). */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String message) {
        super(message);
    }
}
