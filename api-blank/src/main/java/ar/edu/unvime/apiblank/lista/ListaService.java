package ar.edu.unvime.apiblank.lista;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ar.edu.unvime.apiblank.error.*;
import ar.edu.unvime.apiblank.favorito.FavoritoRepository;
import ar.edu.unvime.apiblank.favorito.FavoritoResponse;

@Service
@Transactional(readOnly = true)
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
        return respuesta(buscar(id));
    }

    @Transactional
    public ListaResponse crear(ListaRequest request) {
        return respuesta(repository.crear(request.nombre().trim()));
    }

    public List<FavoritoResponse> favoritos(Long id) {
        buscar(id);
        return favoritos.buscarPorListaId(id).stream().map(FavoritoResponse::desde).toList();
    }

    @Transactional
    public void eliminar(Long id) {
        buscar(id);
        if (favoritos.existenEnLista(id)) {
            throw new ConflictoException("La lista " + id + " todavía tiene favoritos");
        }
        if (!repository.eliminar(id)) throw noEncontrada(id);
    }

    @Transactional
    public void moverFavoritos(Long origenId, MoverFavoritosRequest request) {
        buscar(origenId);
        buscar(request.destinoId());
        if (origenId.equals(request.destinoId())) {
            throw new SolicitudInvalidaException("Las listas de origen y destino deben ser distintas");
        }
        favoritos.moverFavoritos(origenId, request.destinoId());
        if (!repository.eliminar(origenId)) throw noEncontrada(origenId);
    }

    private Lista buscar(Long id) {
        return repository.buscarPorId(id).orElseThrow(() -> noEncontrada(id));
    }

    private RecursoNoEncontradoException noEncontrada(Long id) {
        return new RecursoNoEncontradoException("No existe la lista " + id);
    }

    private ListaResponse respuesta(Lista lista) {
        return new ListaResponse(lista.id(), lista.nombre());
    }
}
