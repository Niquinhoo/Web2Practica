package ar.edu.unvime.apiblank.lista;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ar.edu.unvime.apiblank.error.RecursoNoEncontradoException;
import ar.edu.unvime.apiblank.favorito.FavoritoRepository;
import ar.edu.unvime.apiblank.favorito.FavoritoResponse;

@Service
public class ListaService {
    private final ListaRepository repository;
    private final FavoritoRepository favoritos;

    public ListaService(ListaRepository repository, FavoritoRepository favoritos) {
        this.repository = repository;
        this.favoritos = favoritos;
    }

    public List<ListaResponse> listar() {
        return repository.buscarTodos().stream().map(this::respuesta).toList();
    }

    public ListaResponse obtener(Long id) {
        return respuesta(repository.buscarPorId(id).orElseThrow(() -> noEncontrada(id)));
    }

    public ListaResponse crear(ListaRequest request) {
        return respuesta(repository.crear(request.nombre().strip()));
    }

    public List<FavoritoResponse> favoritos(Long id) {
        obtener(id);
        return favoritos.buscarPorListaId(id).stream().map(f -> new FavoritoResponse(
                f.id(), f.productoId(), f.nota(), f.fechaAgregado(), f.listaId())).toList();
    }

    public void eliminar(Long id) {
        if (!repository.eliminar(id)) {
            throw noEncontrada(id);
        }
    }

    @Transactional
    public void moverFavoritos(Long origenId, Long destinoId) {
        obtener(origenId);
        obtener(destinoId);
        if (origenId.equals(destinoId)) {
            throw new IllegalArgumentException("La lista destino debe ser distinta de la lista origen");
        }
        favoritos.moverDeLista(origenId, destinoId);
        eliminar(origenId);
    }

    private RecursoNoEncontradoException noEncontrada(Long id) {
        return new RecursoNoEncontradoException("No existe la lista " + id);
    }

    private ListaResponse respuesta(Lista lista) {
        return new ListaResponse(lista.id(), lista.nombre());
    }
}
