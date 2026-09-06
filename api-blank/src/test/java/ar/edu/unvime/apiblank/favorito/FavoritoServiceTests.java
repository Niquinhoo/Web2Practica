package ar.edu.unvime.apiblank.favorito;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import ar.edu.unvime.apiblank.error.RecursoNoEncontradoException;

class FavoritoServiceTests {
    private final FavoritoRepository repository = mock(FavoritoRepository.class);
    private final Instant fecha = Instant.parse("2026-09-06T12:00:00Z");
    private final FavoritoService service = new FavoritoService(repository, Clock.fixed(fecha, ZoneOffset.UTC));
    private final Favorito favorito = new Favorito(7L, 3L, "Nota", fecha);

    @Test
    void crearGeneraFechaEnBackendYMapeaRespuesta() {
        when(repository.crear(3L, "Nota", fecha)).thenReturn(favorito);
        assertThat(service.crear(new FavoritoRequest(3L, "Nota")))
                .isEqualTo(new FavoritoResponse(7L, 3L, "Nota", fecha));
        verify(repository).crear(3L, "Nota", fecha);
    }

    @Test
    void listarYObtenerMapeanElDominio() {
        when(repository.buscarTodos()).thenReturn(List.of(favorito));
        when(repository.buscarPorId(7L)).thenReturn(Optional.of(favorito));
        assertThat(service.listar()).containsExactly(service.obtener(7L));
        assertThat(service.obtener(7L).productoId()).isEqualTo(3L);
    }

    @Test
    void actualizarReemplazaDatos() {
        when(repository.actualizar(7L, 9L, null)).thenReturn(Optional.of(new Favorito(7L, 9L, null, fecha)));
        assertThat(service.actualizar(7L, new FavoritoRequest(9L, null)))
                .isEqualTo(new FavoritoResponse(7L, 9L, null, fecha));
        verify(repository).actualizar(7L, 9L, null);
    }

    @Test
    void operacionesSobreInexistentesFallanSinCrear() {
        when(repository.buscarPorId(99L)).thenReturn(Optional.empty());
        when(repository.actualizar(99L, 1L, null)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> service.actualizar(99L, new FavoritoRequest(1L, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> service.eliminar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
        verify(repository, never()).crear(anyLong(), any(), any());
    }

    @Test
    void eliminarDelegaAlRepositorio() {
        when(repository.eliminar(7L)).thenReturn(true);
        service.eliminar(7L);
        verify(repository).eliminar(7L);
    }
}
