package ar.edu.unvime.apiblank.favorito;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ar.edu.unvime.apiblank.error.*;

class FavoritoControllerTests {
    private final FavoritoService service = mock(FavoritoService.class);
    private MockMvc mvc;
    private final FavoritoResponse respuesta = new FavoritoResponse(1L, 3L, null, Instant.parse("2026-09-06T12:00:00Z"), 1L);

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new FavoritoController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void crearDevuelve201YLocation() throws Exception {
        when(service.crear(new FavoritoRequest(3L, null, 1L))).thenReturn(respuesta);
        mvc.perform(post("/api/favoritos").contentType(MediaType.APPLICATION_JSON).content("{\"productoId\":3,\"listaId\":1}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "http://localhost/api/favoritos/1"))
                .andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.fechaAgregado").value("2026-09-06T12:00:00Z"));
    }

    @Test
    void listarObtenerActualizarYEliminar() throws Exception {
        when(service.listar()).thenReturn(List.of(respuesta));
        when(service.obtener(1L)).thenReturn(respuesta);
        when(service.actualizar(1L, new FavoritoRequest(3L, null, 1L))).thenReturn(respuesta);
        mvc.perform(get("/api/favoritos")).andExpect(status().isOk()).andExpect(jsonPath("$[0].productoId").value(3));
        mvc.perform(get("/api/favoritos/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(put("/api/favoritos/1").contentType(MediaType.APPLICATION_JSON).content("{\"productoId\":3,\"listaId\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(delete("/api/favoritos/1")).andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).eliminar(1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"productoId\":0}", "{\"productoId\":-1}", "{\"productoId\":null}"})
    void validaProductoIdEnPostYPut(String json) throws Exception {
        for (var request : List.of(post("/api/favoritos"), put("/api/favoritos/1"))) {
            mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.campos.productoId").exists());
        }
        verifyNoInteractions(service);
    }

    @Test
    void limitaLaNota() throws Exception {
        mvc.perform(post("/api/favoritos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productoId\":1,\"nota\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.nota").exists());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"productoId\":1}", "{\"productoId\":1,\"listaId\":null}",
            "{\"productoId\":1,\"listaId\":0}", "{\"productoId\":1,\"listaId\":-1}"})
    void validaListaIdEnPostYPut(String json) throws Exception {
        for (var request : List.of(post("/api/favoritos"), put("/api/favoritos/1"))) {
            mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.listaId").exists());
        }
        verifyNoInteractions(service);
    }

    @Test
    void inexistentesDevuelven404() throws Exception {
        when(service.obtener(99L)).thenThrow(new RecursoNoEncontradoException("No existe el favorito 99"));
        when(service.actualizar(eq(99L), any())).thenThrow(new RecursoNoEncontradoException("No existe el favorito 99"));
        doThrow(new RecursoNoEncontradoException("No existe el favorito 99")).when(service).eliminar(99L);
        mvc.perform(get("/api/favoritos/99")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(put("/api/favoritos/99").contentType(MediaType.APPLICATION_JSON).content("{\"productoId\":1,\"listaId\":1}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(delete("/api/favoritos/99")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{\"productoId\":\"abc\"}", "null", ""})
    void jsonInvalidoUsaErrorComun(String json) throws Exception {
        mvc.perform(post("/api/favoritos").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").isString()).andExpect(jsonPath("$.campos").isMap());
    }

    @Test
    void erroresDeProtocoloConservanFormatoYHeaders() throws Exception {
        mvc.perform(get("/api/favoritos/abc")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(patch("/api/favoritos/1")).andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow")).andExpect(jsonPath("$.status").value(405));
        mvc.perform(post("/api/favoritos").contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.status").value(415));
    }
}
