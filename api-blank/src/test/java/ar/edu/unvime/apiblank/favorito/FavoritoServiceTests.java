package ar.edu.unvime.apiblank.favorito;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ar.edu.unvime.apiblank.error.RecursoNoEncontradoException;
import ar.edu.unvime.apiblank.lista.*;

class FavoritoServiceTests {
    private final FavoritoRepository repository = mock(FavoritoRepository.class);
    private final ListaRepository listas = mock(ListaRepository.class);
    private final Instant fecha = Instant.parse("2026-09-06T12:00:00Z");
    private final FavoritoService service = new FavoritoService(repository, Clock.fixed(fecha, ZoneOffset.UTC), listas);
    private final Favorito favorito = new Favorito(7L, 3L, "Nota", fecha, 1L);

    @BeforeEach
    void listaExistente() {
        when(listas.buscarPorId(1L)).thenReturn(Optional.of(new Lista(1L, "Regalos")));
    }
    @Test
    void crearGeneraFechaYListaEnRespuesta() {
        when(repository.crear(3L, "Nota", fecha, 1L)).thenReturn(favorito);
        assertThat(service.crear(new FavoritoRequest(3L, "Nota", 1L)))
                .isEqualTo(new FavoritoResponse(7L, 3L, "Nota", fecha, 1L));
    }
    @Test
    void listarYObtenerMapeanElDominio() {
        when(repository.buscarTodos()).thenReturn(List.of(favorito));
        when(repository.buscarPorId(7L)).thenReturn(Optional.of(favorito));
        assertThat(service.listar()).containsExactly(service.obtener(7L));
    }
    @Test
    void actualizarReemplazaDatosConservandoFecha() {
        when(repository.buscarPorId(7L)).thenReturn(Optional.of(favorito));
        when(repository.actualizar(7L, 9L, null, 1L)).thenReturn(Optional.of(new Favorito(7L, 9L, null, fecha, 1L)));
        assertThat(service.actualizar(7L, new FavoritoRequest(9L, null, 1L)))
                .isEqualTo(new FavoritoResponse(7L, 9L, null, fecha, 1L));
    }
    @Test
    void listaInexistenteNoEscribe() {
        assertThatThrownBy(() -> service.crear(new FavoritoRequest(1L, null, 99L)))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessage("No existe la lista 99");
        verifyNoInteractions(repository);
    }
    @Test
    void operacionesSobreInexistentesFallanSinCrear() {
        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> service.actualizar(99L, new FavoritoRequest(1L, null, 1L)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> service.eliminar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
        verify(repository, never()).crear(anyLong(), any(), any(), anyLong());
        verify(repository, never()).actualizar(anyLong(), anyLong(), any(), anyLong());
    }
    @Test
    void eliminarDelegaAlRepositorio() {
        when(repository.eliminar(7L)).thenReturn(true);
        service.eliminar(7L);
        verify(repository).eliminar(7L);
    }
}
