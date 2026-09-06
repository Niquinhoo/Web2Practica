package ar.edu.unvime.apiblank;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;
import ar.edu.unvime.apiblank.producto.*;
import ar.edu.unvime.apiblank.error.ServicioExternoException;

@SpringBootTest
class ApiIntegrationTests {
    @Autowired private WebApplicationContext context;
    @MockitoBean private ProductoClient productoClient;
    private MockMvc mvc;
    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void cicloCompletoDeFavoritosConRepositorioReal() throws Exception {
        var creado = mvc.perform(post("/api/favoritos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productoId\":1,\"nota\":\"Inicial\"}"))
                .andExpect(status().isCreated()).andReturn();
        var datos = json.readTree(creado.getResponse().getContentAsString());
        String ruta = "/api/favoritos/" + datos.get("id").asLong();
        String fecha = datos.get("fechaAgregado").asString();
        mvc.perform(get(ruta)).andExpect(status().isOk()).andExpect(jsonPath("$.nota").value("Inicial"));
        mvc.perform(get("/api/favoritos")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        mvc.perform(put(ruta).contentType(MediaType.APPLICATION_JSON).content("{\"productoId\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.productoId").value(2))
                .andExpect(jsonPath("$.nota").isEmpty()).andExpect(jsonPath("$.fechaAgregado").value(fecha));
        mvc.perform(delete(ruta)).andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get(ruta)).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(put(ruta).contentType(MediaType.APPLICATION_JSON).content("{\"productoId\":2}"))
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
    void swaggerYOpenApiPublicanLosSieteEndpoints() throws Exception {
        var result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var spec = json.readTree(result.getResponse().getContentAsString());
        var paths = spec.get("paths");
        for (String ruta : List.of("/api/productos", "/api/productos/{id}", "/api/favoritos", "/api/favoritos/{id}")) {
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
