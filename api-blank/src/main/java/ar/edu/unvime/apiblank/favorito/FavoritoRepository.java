package ar.edu.unvime.apiblank.favorito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FavoritoRepository {
    List<Favorito> buscarTodos();
    Optional<Favorito> buscarPorId(Long id);
    Favorito crear(Long productoId, String nota, Instant fechaAgregado, Long listaId);
    Optional<Favorito> actualizar(Long id, Long productoId, String nota, Long listaId);
    List<Favorito> buscarPorListaId(Long listaId);
    boolean existenEnLista(Long listaId);
    int moverFavoritos(Long origenId, Long destinoId);
    boolean eliminar(Long id);
}
