package ar.edu.unvime.apiblank;
import static org.assertj.core.api.Assertions.*;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class FlywayMigrationIT {
    @Test
    void evolucionaDatosAnterioresSinPerderFavoritos() throws Exception {
        String schema = PostgresTestDatabase.nuevoEsquema();
        try {
            flyway(schema, "1").migrate();
            PostgresTestDatabase.ejecutar("INSERT INTO " + schema
                    + ".favoritos (producto_id, nota, fecha_alta) VALUES (3, 'TP1 conservado', '2026-09-06T12:00:00Z')");
            flyway(schema, "3").migrate();
            PostgresTestDatabase.ejecutar("INSERT INTO " + schema + ".listas (nombre) VALUES ('Regalos')");
            PostgresTestDatabase.ejecutar("INSERT INTO " + schema
                    + ".favoritos (producto_id, nota, fecha_alta, lista_id) VALUES (4, 'Clasificado', now(), 1)");
            var finalizada = flyway(schema, null);
            finalizada.migrate();
            assertThat(finalizada.info().applied()).hasSize(4);
            assertThat(finalizada.migrate().migrationsExecuted).isZero();
            try (var connection = DriverManager.getConnection(PostgresTestDatabase.URL,
                    PostgresTestDatabase.USER, PostgresTestDatabase.PASSWORD);
                    var statement = connection.createStatement()) {
                var rows = statement.executeQuery("SELECT f.producto_id, f.nota, l.nombre FROM " + schema
                        + ".favoritos f JOIN " + schema + ".listas l ON l.id=f.lista_id ORDER BY f.id");
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong(1)).isEqualTo(3);
                assertThat(rows.getString(2)).isEqualTo("TP1 conservado");
                assertThat(rows.getString(3)).isEqualTo("Sin clasificar");
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(3)).isEqualTo("Regalos");
                assertThat(rows.next()).isFalse();
                var column = statement.executeQuery("SELECT is_nullable FROM information_schema.columns WHERE table_schema='"
                        + schema + "' AND table_name='favoritos' AND column_name='lista_id'");
                assertThat(column.next()).isTrue();
                assertThat(column.getString(1)).isEqualTo("NO");
            }
        } finally { PostgresTestDatabase.borrar(schema); }
    }
    private Flyway flyway(String schema, String target) {
        var config = Flyway.configure().dataSource(PostgresTestDatabase.URL, PostgresTestDatabase.USER,
                PostgresTestDatabase.PASSWORD).schemas(schema).defaultSchema(schema);
        if (target != null) config.target(target);
        return config.load();
    }
}
