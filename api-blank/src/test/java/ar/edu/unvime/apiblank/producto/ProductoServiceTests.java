package ar.edu.unvime.apiblank.producto;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import ar.edu.unvime.apiblank.error.*;

class ProductoServiceTests {
    private final ProductoClient client = mock(ProductoClient.class);
    private final ProductoService service = new ProductoService(client);
    private final ProductoExterno externo = new ProductoExterno(1L, "Producto", "Descripción", new BigDecimal("9.99"), "beauty");

    @Test
    void transformaListadoYDetalleAlContratoPropio() {
        when(client.listar()).thenReturn(List.of(externo));
        when(client.obtener(1L)).thenReturn(Optional.of(externo));
        var esperado = new ProductoResponse(1L, "Producto", "Descripción", new BigDecimal("9.99"), "beauty");
        assertThat(service.listar()).containsExactly(esperado);
        assertThat(service.obtener(1L)).isEqualTo(esperado);
    }

    @Test
    void inexistenteSeTraduceAExcepcionPropia() {
        when(client.obtener(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void fallaExternaConservaSuTipoParaElAdvice() {
        var falla = new ServicioExternoException("timeout");
        when(client.listar()).thenThrow(falla);
        when(client.obtener(1L)).thenThrow(falla);
        assertThatThrownBy(service::listar).isSameAs(falla);
        assertThatThrownBy(() -> service.obtener(1L)).isSameAs(falla);
    }
}
