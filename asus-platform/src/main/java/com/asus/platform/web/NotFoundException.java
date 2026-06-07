package com.asus.platform.web;

/** Lancada quando um recurso nao e encontrado. Mapeada para HTTP 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String mensagem) {
        super(mensagem);
    }
}
