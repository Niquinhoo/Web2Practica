package ar.edu.unvime.apiblank.lista;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import ar.edu.unvime.apiblank.error.*;
import ar.edu.unvime.apiblank.favorito.FavoritoRepository;

class ListaServiceTests {
    private final ListaRepository listas = mock(ListaRepository.class);
    private final FavoritoRepository favoritos = mock(FavoritoRepository.class);
    private final ListaService service = new ListaService(listas, favoritos);

    private void existente(Long id) {
        when(listas.buscarPorId(id)).thenReturn(Optional.of(new Lista(id, "Lista " + id)));
    }
    @Test
    void noEliminaListaConFavoritos() {
        existente(1L);
        when(favoritos.existenEnLista(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.eliminar(1L)).isInstanceOf(ConflictoException.class);
        verify(listas, never()).eliminar(anyLong());
    }
    @Test
    void moverValidaAmbasListasAntesDeEscribir() {
        existente(1L);
        assertThatThrownBy(() -> service.moverFavoritos(1L, new MoverFavoritosRequest(99L)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> service.moverFavoritos(99L, new MoverFavoritosRequest(1L)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(favoritos);
        verify(listas, never()).eliminar(anyLong());
    }
    @Test
    void noPermiteMoverSobreLaMismaLista() {
        existente(1L);
        assertThatThrownBy(() -> service.moverFavoritos(1L, new MoverFavoritosRequest(1L)))
                .isInstanceOf(SolicitudInvalidaException.class);
        verifyNoInteractions(favoritos);
    }
    @Test
    void mueveAntesDeEliminarOrigen() {
        existente(1L);
        existente(2L);
        when(listas.eliminar(1L)).thenReturn(true);
        service.moverFavoritos(1L, new MoverFavoritosRequest(2L));
        var orden = inOrder(favoritos, listas);
        orden.verify(favoritos).moverFavoritos(1L, 2L);
        orden.verify(listas).eliminar(1L);
    }
}
