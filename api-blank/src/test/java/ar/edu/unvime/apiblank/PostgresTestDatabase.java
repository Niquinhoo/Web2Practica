package ar.edu.unvime.apiblank;
import java.sql.*;
import java.util.UUID;
import org.springframework.test.context.DynamicPropertyRegistry;

final class PostgresTestDatabase {
    static final String URL = variable("TEST_DATABASE_URL", "jdbc:postgresql://localhost:5432/webii_tp2");
    static final String USER = variable("TEST_DATABASE_USER", "webii_tp2");
    static final String PASSWORD = variable("TEST_DATABASE_PASSWORD", "webii_tp2");

    static String nuevoEsquema() throws SQLException {
        String schema = "tp2_test_" + UUID.randomUUID().toString().replace("-", "");
        ejecutar("CREATE SCHEMA " + schema);
        return schema;
    }
    static void ejecutar(String sql) throws SQLException {
        try (var connection = DriverManager.getConnection(URL, USER, PASSWORD);
                var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
    static void configurar(DynamicPropertyRegistry properties, String schema) {
        properties.add("spring.datasource.url", () -> URL + (URL.contains("?") ? "&" : "?") + "currentSchema=" + schema);
        properties.add("spring.datasource.username", () -> USER);
        properties.add("spring.datasource.password", () -> PASSWORD);
        properties.add("spring.flyway.default-schema", () -> schema);
    }
    static void borrar(String schema) throws SQLException {
        ejecutar("DROP SCHEMA " + schema + " CASCADE");
    }
    private static String variable(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
