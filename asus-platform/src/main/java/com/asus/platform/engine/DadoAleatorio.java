package com.asus.platform.engine;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/** Implementacao padrao do {@link Dado} usando RNG por thread. */
@Component
public class DadoAleatorio implements Dado {

    @Override
    public int rolar(int faces) {
        if (faces < 1) {
            throw new IllegalArgumentException("faces deve ser >= 1");
        }
        return ThreadLocalRandom.current().nextInt(faces) + 1;
    }
}
