package com.asus.platform.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Registro de engines (plano, secao 5.3).
 *
 * <p>Coleta todos os {@link GameSystemEngine} do contexto Spring e os indexa
 * por "SYSTEM_ID::VERSION". O ASUS_V1 e o engine padrao.</p>
 */
@Component
public class GameSystemRegistry {

    private final Map<String, GameSystemEngine> engines = new HashMap<>();

    public GameSystemRegistry(List<GameSystemEngine> todosEngines) {
        for (GameSystemEngine engine : todosEngines) {
            engines.put(chave(engine.getSystemId(), engine.getVersion()), engine);
        }
    }

    public GameSystemEngine getEngine(String systemId, String version) {
        GameSystemEngine engine = engines.get(chave(systemId, version));
        if (engine == null) {
            throw new IllegalArgumentException(
                    "Nenhum engine registrado para " + systemId + " / " + version);
        }
        return engine;
    }

    private String chave(String systemId, String version) {
        return systemId + "::" + version;
    }
}
