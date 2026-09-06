package ar.edu.unvime.apiblank.producto;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ar.edu.unvime.apiblank.error.*;

class ProductoControllerTests {
    private final ProductoService service = mock(ProductoService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProductoController(service))
            .setControllerAdvice(new ApiExceptionHandler()).build();

    @Test
    void listadoYDetalleSoloExponenDtoPropio() throws Exception {
        var producto = new ProductoResponse(1L, "Producto", "Descripción", new BigDecimal("9.99"), "beauty");
        when(service.listar()).thenReturn(List.of(producto));
        when(service.obtener(1L)).thenReturn(producto);
        mvc.perform(get("/api/productos")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Producto")).andExpect(jsonPath("$[0].title").doesNotExist());
        mvc.perform(get("/api/productos/1")).andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":1,"nombre":"Producto","descripcion":"Descripción","precio":9.99,"categoria":"beauty"}
                        """)).andExpect(jsonPath("$.reviews").doesNotExist());
    }

    @Test
    void inexistenteYProveedorCaidoUsanErrorComun() throws Exception {
        when(service.obtener(99L)).thenThrow(new RecursoNoEncontradoException("No existe el producto 99"));
        when(service.listar()).thenThrow(new ServicioExternoException("Detalle privado"));
        mvc.perform(get("/api/productos/99")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(get("/api/productos")).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502)).andExpect(jsonPath("$.campos").isMap())
                .andExpect(jsonPath("$.mensaje").value("No se pudo consultar el catálogo externo. Intentá nuevamente más tarde."));
    }

    @Test
    void formatoInvalidoYEscrituraNoPermitida() throws Exception {
        mvc.perform(get("/api/productos/abc")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(post("/api/productos")).andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.status").value(405));
        verifyNoInteractions(service);
    }
}
