package ar.edu.unvime.apiblank.config;

import java.time.Clock;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class ApiConfig {
    @Bean
    OpenApiCustomizer ejemplosDeErrores() {
        return api -> api.getPaths().values().forEach(path -> path.readOperations().forEach(operation ->
                operation.getResponses().forEach((codigo, response) -> {
                    if (response.getContent() != null && (codigo.equals("400")
                            || codigo.equals("404") || codigo.equals("409") || codigo.equals("502"))) {
                        response.getContent().values().forEach(media -> media.setExample(Map.of(
                                "status", Integer.parseInt(codigo),
                                "mensaje", response.getDescription(),
                                "campos", Map.of())));
                    }
                })));
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("TP2 · Productos, favoritos y listas").version("2.0.0")
                .description("Catálogo de DummyJSON de solo lectura y favoritos y listas en PostgreSQL. "
                        + "Migraciones Flyway y movimiento transaccional entre listas."));
    }
}
