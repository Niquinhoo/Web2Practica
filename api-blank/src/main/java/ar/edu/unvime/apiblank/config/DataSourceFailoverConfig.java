package ar.edu.unvime.apiblank.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Elige la base al iniciar: primero la principal (el Tunnel) y, si no responde, la de respaldo (Render).
 * Solo se activa si {@code app.datasource.fallback.url} (DATABASE_FALLBACK_URL) no está vacía; sin ella rige la configuración habitual.
 * La elección se hace una vez por arranque: Flyway y JPA usan la base elegida, y las dos bases son
 * independientes (no se replican datos entre ellas).
 */
@Configuration
@ConditionalOnExpression("!'${app.datasource.fallback.url:}'.isBlank()")
public class DataSourceFailoverConfig {
    private static final Logger log = LoggerFactory.getLogger(DataSourceFailoverConfig.class);

    @Bean(destroyMethod = "close")
    DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String usuario,
            @Value("${spring.datasource.password}") String password,
            @Value("${app.datasource.fallback.url}") String respaldoUrl,
            @Value("${app.datasource.fallback.username}") String respaldoUsuario,
            @Value("${app.datasource.fallback.password}") String respaldoPassword,
            @Value("${app.datasource.primary-timeout-seconds:5}") int segundos,
            @Value("${spring.datasource.hikari.minimum-idle:10}") int minimoInactivas) {
        try {
            var principal = crear(url, usuario, password, segundos, minimoInactivas);
            log.info("Base principal disponible; se usa la conexión principal");
            return principal;
        } catch (RuntimeException e) {
            log.warn("Base principal no disponible ({}); se usa la base de respaldo", e.getClass().getSimpleName());
            return crear(respaldoUrl, respaldoUsuario, respaldoPassword, Math.max(segundos, 15), minimoInactivas);
        }
    }

    static HikariDataSource crear(String url, String usuario, String password, int segundos, int minimoInactivas) {
        var config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(usuario);
        config.setPassword(password);
        // Este bean no pasa por el binding de spring.datasource.hikari: se aplica a mano.
        config.setMinimumIdle(minimoInactivas);
        config.setConnectionTimeout(segundos * 1000L);
        // Con un valor positivo, el pool prueba una conexión real al crearse y falla si no la logra.
        config.setInitializationFailTimeout(segundos * 1000L);
        config.addDataSourceProperty("connectTimeout", String.valueOf(segundos));
        config.addDataSourceProperty("loginTimeout", String.valueOf(segundos));
        return new HikariDataSource(config);
    }
}
