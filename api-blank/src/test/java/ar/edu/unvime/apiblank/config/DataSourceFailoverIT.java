package ar.edu.unvime.apiblank.config;

import static org.assertj.core.api.Assertions.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import com.zaxxer.hikari.HikariDataSource;
import ar.edu.unvime.apiblank.PostgresTestDatabase;

class DataSourceFailoverIT {
    private static final String URL_CAIDA = "jdbc:postgresql://127.0.0.1:1/inexistente";
    private final DataSourceFailoverConfig config = new DataSourceFailoverConfig();

    private DataSource elegir(String principal, String respaldo) {
        return config.dataSource(principal, PostgresTestDatabase.USER, PostgresTestDatabase.PASSWORD,
                respaldo, PostgresTestDatabase.USER, PostgresTestDatabase.PASSWORD, 2);
    }

    @Test
    void elegir_principalDisponible_usaPrincipal() throws Exception {
        try (var ds = (HikariDataSource) elegir(PostgresTestDatabase.URL, URL_CAIDA)) {
            assertThat(ds.getJdbcUrl()).isEqualTo(PostgresTestDatabase.URL);
            try (var c = ds.getConnection()) { assertThat(c.isValid(2)).isTrue(); }
        }
    }

    @Test
    void elegir_principalCaida_usaRespaldo() throws Exception {
        try (var ds = (HikariDataSource) elegir(URL_CAIDA, PostgresTestDatabase.URL)) {
            assertThat(ds.getJdbcUrl()).isEqualTo(PostgresTestDatabase.URL);
            try (var c = ds.getConnection()) { assertThat(c.isValid(2)).isTrue(); }
        }
    }

    @Test
    void elegir_ambasCaidas_lanzaError() {
        assertThatThrownBy(() -> elegir(URL_CAIDA, URL_CAIDA)).isInstanceOf(RuntimeException.class);
    }
}
