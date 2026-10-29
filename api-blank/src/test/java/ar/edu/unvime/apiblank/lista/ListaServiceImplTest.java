package ar.edu.unvime.apiblank.lista;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import ar.edu.unvime.apiblank.error.*;
import ar.edu.unvime.apiblank.favorito.FavoritoRepository;

class ListaServiceImplTest {
    private final ListaRepository listas = mock(ListaRepository.class);
    private final FavoritoRepository favoritos = mock(FavoritoRepository.class);
    private final ar.edu.unvime.apiblank.seguridad.ContextoSeguridad seguridad = mock(ar.edu.unvime.apiblank.seguridad.ContextoSeguridad.class);
    private final ListaServiceImpl service = new ListaServiceImpl(listas, favoritos, seguridad);

    @Test
    void crear_usuarioAutenticado_asignaPropiedad() {
        when(seguridad.usuarioId()).thenReturn(10L);
        when(listas.crear("Regalos", 10L)).thenReturn(new Lista(1L, "Regalos", 10L));
        assertThat(service.crear(new ListaRequest(" Regalos ")).usuarioId()).isEqualTo(10L);
        verify(listas).crear("Regalos", 10L);
    }
    private void existente(Long id) {
        when(listas.buscarPorId(id)).thenReturn(Optional.of(new Lista(id, "Lista " + id, 10L)));
    }
    @Test
    void eliminar_listaConFavoritos_lanzaConflictoSinBorrar() {
        existente(1L);
        when(favoritos.existenEnLista(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.eliminar(1L)).isInstanceOf(ConflictoException.class);
        verify(listas, never()).eliminar(anyLong());
    }
    @Test
    void mover_listaInexistente_rechazaAntesDeEscribir() {
        existente(1L);
        assertThatThrownBy(() -> service.moverFavoritos(1L, new MoverFavoritosRequest(99L)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> service.moverFavoritos(99L, new MoverFavoritosRequest(1L)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(favoritos);
        verify(listas, never()).eliminar(anyLong());
    }
    @Test
    void mover_mismaLista_rechazaSinEscribir() {
        existente(1L);
        assertThatThrownBy(() -> service.moverFavoritos(1L, new MoverFavoritosRequest(1L)))
                .isInstanceOf(SolicitudInvalidaException.class);
        verifyNoInteractions(favoritos);
    }
    @Test
    void mover_listasValidas_mueveAntesDeEliminarOrigen() {
        existente(1L);
        existente(2L);
        when(listas.eliminar(1L)).thenReturn(true);
        service.moverFavoritos(1L, new MoverFavoritosRequest(2L));
        var orden = inOrder(favoritos, listas);
        orden.verify(favoritos).moverFavoritos(1L, 2L);
        orden.verify(listas).eliminar(1L);
    }
}
