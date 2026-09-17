package ar.edu.unvime.apiblank;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;
import ar.edu.unvime.apiblank.favorito.FavoritoRepository;
import ar.edu.unvime.apiblank.lista.*;

@SpringBootTest
@ActiveProfiles("test")
class Tp2IntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired ListaService listas;
    @Autowired FavoritoRepository favoritos;
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    private MockMvc mvc;
    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void listasValidacionesConflictoYMovimientoAtomico() throws Exception {
        long origen = crearLista("Regalos");
        long destino = crearLista("Ofertas");
        try {
            mvc.perform(get("/api/listas")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
            mvc.perform(get("/api/listas/" + origen)).andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Regalos"));
            for (String body : new String[]{"{}", "{\"nombre\":\"   \"}", "{\"nombre\":\"" + "a".repeat(256) + "\"}"}) {
                mvc.perform(post("/api/listas").contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.nombre").exists());
            }
            var primero = favoritos.crear(1L, "Uno", Instant.now(), origen);
            var segundo = favoritos.crear(2L, null, Instant.now(), origen);
            var existente = favoritos.crear(3L, "Ya en destino", Instant.now(), destino);
            mvc.perform(get("/api/listas/" + origen + "/favoritos"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
            mvc.perform(delete("/api/listas/" + origen)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
            mvc.perform(post("/api/listas/" + origen + "/mover-favoritos").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"destinoId\":" + origen + "}")).andExpect(status().isBadRequest());
            for (String body : new String[]{"{}", "{\"destinoId\":0}", "{\"destinoId\":-1}"}) {
                mvc.perform(post("/api/listas/" + origen + "/mover-favoritos").contentType(MediaType.APPLICATION_JSON)
                        .content(body)).andExpect(status().isBadRequest());
            }
            mvc.perform(post("/api/listas/" + origen + "/mover-favoritos").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"destinoId\":9223372036854775807}")).andExpect(status().isNotFound());
            mvc.perform(post("/api/listas/9223372036854775807/mover-favoritos").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"destinoId\":" + destino + "}")).andExpect(status().isNotFound());
            assertThat(favoritos.buscarPorListaId(origen)).hasSize(2);
            mvc.perform(post("/api/listas/" + origen + "/mover-favoritos").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"destinoId\":" + destino + "}")).andExpect(status().isNoContent());
            mvc.perform(get("/api/listas/" + origen)).andExpect(status().isNotFound());
            mvc.perform(get("/api/listas/" + origen + "/favoritos")).andExpect(status().isNotFound());
            mvc.perform(delete("/api/listas/" + origen)).andExpect(status().isNotFound());
            assertThat(favoritos.buscarPorListaId(destino)).extracting(f -> f.id())
                    .containsExactly(primero.id(), segundo.id(), existente.id());
            assertThat(favoritos.buscarPorId(primero.id()).orElseThrow().fechaAgregado()).isEqualTo(primero.fechaAgregado());
            assertThat(favoritos.buscarPorId(primero.id()).orElseThrow().nota()).isEqualTo("Uno");
            for (var favorito : favoritos.buscarPorListaId(destino)) favoritos.eliminar(favorito.id());
            mvc.perform(delete("/api/listas/" + destino)).andExpect(status().isNoContent());
        } finally {
            limpiar(origen, destino);
        }
    }

    @Test
    void favoritosExigenListaExistenteYPermitenCambiarlaSinCambiarFecha() throws Exception {
        long origen = crearLista("Origen");
        long destino = crearLista("Destino");
        try {
            for (String lista : new String[]{"null", "0", "-1"}) {
                mvc.perform(post("/api/favoritos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":1,\"listaId\":" + lista + "}"))
                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.listaId").exists());
            }
            mvc.perform(post("/api/favoritos").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"productoId\":1,\"listaId\":9223372036854775807}"))
                    .andExpect(status().isNotFound());
            var favorito = favoritos.crear(1L, null, Instant.now(), origen);
            mvc.perform(put("/api/favoritos/" + favorito.id()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"productoId\":2,\"listaId\":" + destino + "}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.listaId").value(destino));
            assertThat(favoritos.buscarPorId(favorito.id()).orElseThrow().fechaAgregado()).isEqualTo(favorito.fechaAgregado());
            assertThat(favoritos.buscarPorListaId(origen)).isEmpty();
            // También se puede mover y eliminar una lista vacía.
            listas.moverFavoritos(origen, destino);
            assertThat(favoritos.buscarPorListaId(destino)).hasSize(1);
        } finally {
            limpiar(origen, destino);
        }
    }

    @Test
    void falloRealAlBorrarOrigenRevierteLaReasignacion() {
        long origen = listas.crear(new ListaRequest("Rollback origen")).id();
        long destino = listas.crear(new ListaRequest("Rollback destino")).id();
        var favorito = favoritos.crear(1L, "Conservar", Instant.now(), origen);
        try {
            // Falla en PostgreSQL DESPUÉS del UPDATE, sin transacción exterior de test.
            jdbc.execute("""
                    CREATE FUNCTION tp2_test.rechazar_borrado() RETURNS trigger LANGUAGE plpgsql AS $$
                    BEGIN RAISE EXCEPTION 'Fallo de escritura simulado' USING ERRCODE = '23514'; END $$
                    """);
            jdbc.execute("CREATE TRIGGER fallo_tp2 BEFORE DELETE ON tp2_test.listas FOR EACH ROW "
                    + "WHEN (OLD.id = " + origen + ") EXECUTE FUNCTION tp2_test.rechazar_borrado()");
            assertThatThrownBy(() -> listas.moverFavoritos(origen, destino))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThat(favoritos.buscarPorId(favorito.id()).orElseThrow().listaId()).isEqualTo(origen);
            assertThat(listas.obtener(origen).id()).isEqualTo(origen);
            assertThat(favoritos.buscarPorListaId(destino)).isEmpty();
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS fallo_tp2 ON tp2_test.listas");
            jdbc.execute("DROP FUNCTION IF EXISTS tp2_test.rechazar_borrado()");
            limpiar(origen, destino);
        }
    }

    @Test
    void migracionesConservanFavoritosPreviosYListasYaAsignadas() {
        String schema = "tp2_migration_" + UUID.randomUUID().toString().replace("-", "");
        try {
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target("1").load().migrate();
            jdbc.update("INSERT INTO " + schema + ".favoritos(producto_id,nota,fecha_alta) VALUES (1,'Anterior',now())");
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target("3").load().migrate();
            Long lista = jdbc.queryForObject("INSERT INTO " + schema + ".listas(nombre) VALUES ('Existente') RETURNING id", Long.class);
            jdbc.update("INSERT INTO " + schema + ".favoritos(producto_id,fecha_alta,lista_id) VALUES (2,now(),?)", lista);
            var flyway = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load();
            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT l.nombre FROM " + schema + ".favoritos f JOIN " + schema
                    + ".listas l ON f.lista_id=l.id WHERE f.producto_id=1", String.class)).isEqualTo("Sin clasificar");
            assertThat(jdbc.queryForObject("SELECT lista_id FROM " + schema + ".favoritos WHERE producto_id=2", Long.class)).isEqualTo(lista);
            assertThatThrownBy(() -> jdbc.update("INSERT INTO " + schema + ".favoritos(producto_id,fecha_alta) VALUES (3,now())"))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            flyway.validate();
            assertThat(flyway.migrate().migrationsExecuted).isZero();
        } finally {
            jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    private long crearLista(String nombre) throws Exception {
        var result = mvc.perform(post("/api/listas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"" + nombre + "\"}"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void limpiar(long... ids) {
        for (long id : ids) {
            jdbc.update("DELETE FROM tp2_test.favoritos WHERE lista_id=?", id);
            jdbc.update("DELETE FROM tp2_test.listas WHERE id=?", id);
        }
    }
}
