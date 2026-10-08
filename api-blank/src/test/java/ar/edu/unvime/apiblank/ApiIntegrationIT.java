package ar.edu.unvime.apiblank;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;
import ar.edu.unvime.apiblank.producto.*;
import ar.edu.unvime.apiblank.error.ServicioExternoException;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ApiIntegrationIT {
    private static String schema;
    @Autowired private JdbcTemplate jdbc;

    @BeforeAll
    static void baseReal() throws Exception {
        schema = PostgresTestDatabase.nuevoEsquema();
    }

    @DynamicPropertySource
    static void baseDePrueba(DynamicPropertyRegistry properties) {
        PostgresTestDatabase.configurar(properties, schema);
    }

    @AfterAll
    static void limpiar() throws Exception {
        if (schema != null) PostgresTestDatabase.borrar(schema);
    }
    @Autowired private WebApplicationContext context;
    @MockitoBean private ProductoClient productoClient;
    private MockMvc mvc;
    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private long crearLista(String nombre) throws Exception {
        var result = mvc.perform(post("/api/listas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"" + nombre + "\"}"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long crearFavorito(long listaId) throws Exception {
        var result = mvc.perform(post("/api/favoritos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productoId\":1,\"listaId\":" + listaId + "}"))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void listasCrudYConflictoDeEliminacion() throws Exception {
        long id = crearLista("Regalos");
        mvc.perform(get("/api/listas")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        mvc.perform(get("/api/listas/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Regalos"));
        mvc.perform(get("/api/listas/" + id + "/favoritos")).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        long favorito = crearFavorito(id);
        mvc.perform(delete("/api/listas/" + id)).andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        mvc.perform(get("/api/listas/" + id + "/favoritos")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(favorito));
        mvc.perform(delete("/api/favoritos/" + favorito)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/listas/" + id)).andExpect(status().isNoContent()).andExpect(content().string(""));
        for (String ruta : List.of("/api/listas/" + id, "/api/listas/" + id + "/favoritos")) {
            mvc.perform(get(ruta)).andExpect(status().isNotFound());
        }
        mvc.perform(delete("/api/listas/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void validaEntradasYReferenciasDeListas() throws Exception {
        for (String body : List.of("{}", "{\"nombre\":\"  \"}", "{\"nombre\":\"" + "a".repeat(101) + "\"}")) {
            mvc.perform(post("/api/listas").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.nombre").exists());
        }
        for (String body : List.of("{\"productoId\":1}", "{\"productoId\":1,\"listaId\":0}")) {
            mvc.perform(post("/api/favoritos").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.listaId").exists());
        }
        mvc.perform(post("/api/favoritos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productoId\":1,\"listaId\":9223372036854775807}"))
                .andExpect(status().isNotFound());
        long origen = crearLista("Validación");
        for (String body : List.of("{}", "{\"destinoId\":0}", "{\"destinoId\":-1}")) {
            mvc.perform(post("/api/listas/" + origen + "/mover-favoritos").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/listas/" + origen + "/mover-favoritos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinoId\":" + origen + "}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/listas/" + origen + "/mover-favoritos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinoId\":9223372036854775807}")).andExpect(status().isNotFound());
        mvc.perform(post("/api/listas/9223372036854775807/mover-favoritos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinoId\":" + origen + "}")).andExpect(status().isNotFound());
    }

    @Test
    void mueveTodosLosFavoritosYEliminaOrigen() throws Exception {
        long origen = crearLista("Origen");
        long destino = crearLista("Destino");
        long primero = crearFavorito(origen);
        long segundo = crearFavorito(origen);
        crearFavorito(destino);
        var fecha = jdbc.queryForObject("SELECT fecha_alta FROM favoritos WHERE id=?", java.time.OffsetDateTime.class, primero);
        mvc.perform(post("/api/listas/" + origen + "/mover-favoritos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinoId\":" + destino + "}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get("/api/listas/" + origen)).andExpect(status().isNotFound());
        mvc.perform(get("/api/listas/" + destino + "/favoritos")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        for (long id : List.of(primero, segundo)) {
            mvc.perform(get("/api/favoritos/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.listaId").value(destino));
        }
        assertThat(jdbc.queryForObject("SELECT fecha_alta FROM favoritos WHERE id=?", java.time.OffsetDateTime.class, primero)).isEqualTo(fecha);
    }

    @Test
    void moverListaVaciaTambienEliminaOrigen() throws Exception {
        long origen = crearLista("Vacía");
        long destino = crearLista("Destino vacío");
        mvc.perform(post("/api/listas/" + origen + "/mover-favoritos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinoId\":" + destino + "}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/listas/" + origen)).andExpect(status().isNotFound());
    }

    @Test
    void falloRealTrasMoverRevierteTodaLaTransaccion() throws Exception {
        long origen = crearLista("Rollback origen");
        long destino = crearLista("Rollback destino");
        long favorito = crearFavorito(origen);
        // Una segunda FK impide borrar el origen después de ejecutar el UPDATE.
        jdbc.execute("CREATE TABLE bloqueo_borrado (lista_id BIGINT REFERENCES listas(id))");
        jdbc.update("INSERT INTO bloqueo_borrado VALUES (?)", origen);
        try {
            mvc.perform(post("/api/listas/" + origen + "/mover-favoritos").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"destinoId\":" + destino + "}"))
                    .andExpect(status().isConflict());
            mvc.perform(get("/api/listas/" + origen)).andExpect(status().isOk());
            mvc.perform(get("/api/favoritos/" + favorito)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.listaId").value(origen));
            mvc.perform(get("/api/listas/" + destino + "/favoritos")).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        } finally { jdbc.execute("DROP TABLE bloqueo_borrado"); }
    }

    @Test
    void cicloCompletoDeFavoritosConRepositorioReal() throws Exception {
        long listaId = crearLista("CRUD");
        var creado = mvc.perform(post("/api/favoritos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productoId\":1,\"nota\":\"Inicial\",\"listaId\":" + listaId + "}"))
                .andExpect(status().isCreated()).andReturn();
        var datos = json.readTree(creado.getResponse().getContentAsString());
        String ruta = "/api/favoritos/" + datos.get("id").asLong();
        String fecha = datos.get("fechaAgregado").asString();
        mvc.perform(get(ruta)).andExpect(status().isOk()).andExpect(jsonPath("$.nota").value("Inicial"));
        mvc.perform(get("/api/favoritos")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        mvc.perform(put(ruta).contentType(MediaType.APPLICATION_JSON).content("{\"productoId\":2,\"listaId\":" + listaId + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.productoId").value(2))
                .andExpect(jsonPath("$.nota").isEmpty()).andExpect(jsonPath("$.fechaAgregado").value(fecha));
        mvc.perform(delete(ruta)).andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get(ruta)).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(put(ruta).contentType(MediaType.APPLICATION_JSON).content("{\"productoId\":2,\"listaId\":" + listaId + "}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete(ruta)).andExpect(status().isNotFound());
        mvc.perform(post("/api/favoritos").contentType(MediaType.APPLICATION_JSON).content("{\"productoId\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.productoId").exists());
        verifyNoInteractions(productoClient);
    }

    @Test
    void productosAtraviesanControllerServiceYAdviceSinInternet() throws Exception {
        var producto = new ProductoExterno(1L, "Prueba", "Descripción", new BigDecimal("9.99"), "beauty");
        when(productoClient.listar()).thenReturn(List.of(producto));
        when(productoClient.obtener(1L)).thenReturn(Optional.of(producto));
        when(productoClient.obtener(99L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/productos")).andExpect(status().isOk()).andExpect(jsonPath("$[0].nombre").value("Prueba"));
        mvc.perform(get("/api/productos/1")).andExpect(status().isOk()).andExpect(jsonPath("$.precio").value(9.99));
        mvc.perform(get("/api/productos/99")).andExpect(status().isNotFound());
        when(productoClient.listar()).thenThrow(new ServicioExternoException("timeout simulado"));
        mvc.perform(get("/api/productos")).andExpect(status().isBadGateway()).andExpect(jsonPath("$.status").value(502));
    }

    @Test
    void swaggerYOpenApiPublicanProductosFavoritosYListas() throws Exception {
        var result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var spec = json.readTree(result.getResponse().getContentAsString());
        var paths = spec.get("paths");
        for (String ruta : List.of("/api/productos", "/api/productos/{id}", "/api/favoritos", "/api/favoritos/{id}", "/api/listas", "/api/listas/{id}", "/api/listas/{id}/favoritos", "/api/listas/{origenId}/mover-favoritos")) {
            assertThat(paths.has(ruta)).as(ruta).isTrue();
        }
        assertThat(paths.at("/~1api~1favoritos/post/responses").has("201")).isTrue();
        assertThat(paths.at("/~1api~1favoritos/post/responses").has("200")).isFalse();
        assertThat(paths.at("/~1api~1favoritos~1{id}/delete/responses").has("204")).isTrue();
        assertThat(paths.at("/~1api~1productos/get/responses").has("502")).isTrue();
        assertThat(paths.at("/~1api~1productos/get/tags/0").asString()).isEqualTo("Productos");
        assertThat(paths.at("/~1api~1favoritos/get/tags/0").asString()).isEqualTo("Favoritos");
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Swagger UI")));
        mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk());
    }
}
