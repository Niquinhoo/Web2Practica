package ar.edu.unvime.apiblank.favorito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FavoritoRepository {
    List<Favorito> buscarTodos();
    Optional<Favorito> buscarPorId(Long id);
    Favorito crear(Long productoId, String nota, Instant fechaAgregado, Long listaId);
    Optional<Favorito> actualizar(Long id, Long productoId, String nota, Long listaId);
    boolean eliminar(Long id);
    List<Favorito> buscarPorListaId(Long listaId);
    void moverDeLista(Long origenId, Long destinoId);
}
