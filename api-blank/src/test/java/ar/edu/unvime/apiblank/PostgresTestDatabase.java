package ar.edu.unvime.apiblank;
import java.sql.*;
import java.util.UUID;
import org.springframework.test.context.DynamicPropertyRegistry;

final class PostgresTestDatabase {
    private static final org.testcontainers.postgresql.PostgreSQLContainer POSTGRES = iniciar();
    static final String URL = POSTGRES.getJdbcUrl();
    static final String USER = POSTGRES.getUsername();
    static final String PASSWORD = POSTGRES.getPassword();
    private static org.testcontainers.postgresql.PostgreSQLContainer iniciar() {
        var postgres = new org.testcontainers.postgresql.PostgreSQLContainer("postgres:17");
        postgres.start();
        return postgres;
    }

    static String nuevoEsquema() throws SQLException {
        String schema = "tp3_test_" + UUID.randomUUID().toString().replace("-", "");
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
}
