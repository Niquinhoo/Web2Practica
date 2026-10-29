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
        return new OpenAPI().components(new io.swagger.v3.oas.models.Components().addSecuritySchemes("bearerAuth",
                new io.swagger.v3.oas.models.security.SecurityScheme().type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                        .scheme("bearer").bearerFormat("JWT"))).info(new Info().title("TP3 · Autenticación, listas y favoritos").version("3.0.0")
                .description("Catálogo de DummyJSON de solo lectura. Favoritos y listas persistidos "
                        + "en PostgreSQL, migraciones Flyway y movimiento transaccional de favoritos."));
    }
}
