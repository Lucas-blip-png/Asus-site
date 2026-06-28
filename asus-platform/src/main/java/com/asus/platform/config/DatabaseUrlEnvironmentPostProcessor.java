package com.asus.platform.config;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Converte a {@code DATABASE_URL} no formato do Railway/Heroku
 * ({@code postgresql://usuario:senha@host:porta/banco}) para o datasource JDBC
 * do Spring. Assim, em produção basta setar <b>uma</b> variável no Railway:
 * {@code DATABASE_URL=${{Postgres.DATABASE_URL}}} — sem precisar montar
 * DB_URL/DB_USER/DB_PASSWORD na mão (que dependem das PG* e costumam falhar).
 *
 * <p>Se {@code DATABASE_URL} não estiver definida (ex.: dev/H2/testes), não faz
 * nada — a configuração normal (DB_URL/perfil) continua valendo.</p>
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
        String databaseUrl = env.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        if (!databaseUrl.startsWith("postgres://") && !databaseUrl.startsWith("postgresql://")) {
            return; // formato inesperado: deixa a config normal cuidar
        }
        try {
            URI uri = new URI(databaseUrl);
            String user = "";
            String pass = "";
            String userInfo = uri.getUserInfo();
            if (userInfo != null) {
                int sep = userInfo.indexOf(':');
                user = sep >= 0 ? userInfo.substring(0, sep) : userInfo;
                pass = sep >= 0 ? userInfo.substring(sep + 1) : "";
            }
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String db = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
            String jdbc = "jdbc:postgresql://" + uri.getHost() + ":" + port + "/" + db;

            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", jdbc);
            props.put("spring.datasource.username", user);
            props.put("spring.datasource.password", pass);
            props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            env.getPropertySources().addFirst(new MapPropertySource("railwayDatabaseUrl", props));
        } catch (Exception ignored) {
            // URL invalida: mantem a config normal (DB_URL) como fallback
        }
    }
}
