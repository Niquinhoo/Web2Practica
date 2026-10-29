package ar.edu.unvime.apiblank.lista;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ar.edu.unvime.apiblank.error.*;
import ar.edu.unvime.apiblank.favorito.FavoritoRepository;
import ar.edu.unvime.apiblank.favorito.FavoritoResponse;

@Service
@Transactional(readOnly = true)
public class ListaServiceImpl {
    private final ListaRepository repository;
    private final FavoritoRepository favoritos;

    private final ar.edu.unvime.apiblank.seguridad.ContextoSeguridad seguridad;

    public ListaServiceImpl(ListaRepository repository, FavoritoRepository favoritos,
            ar.edu.unvime.apiblank.seguridad.ContextoSeguridad seguridad) {
        this.repository = repository;
        this.favoritos = favoritos;
        this.seguridad = seguridad;
    }

    public List<ListaResponse> listar() {
        return (seguridad.esAdmin() ? repository.buscarTodos() : repository.buscarPorUsuarioId(seguridad.usuarioId())).stream().map(this::respuesta).toList();
    }

    public ListaResponse obtener(Long id) {
        return respuesta(buscarAutorizada(id));
    }

    @Transactional
    public ListaResponse crear(ListaRequest request) {
        return respuesta(repository.crear(request.nombre().trim(), seguridad.usuarioId()));
    }

    public List<FavoritoResponse> favoritos(Long id) {
        buscarAutorizada(id);
        return favoritos.buscarPorListaId(id).stream().map(FavoritoResponse::desde).toList();
    }

    @Transactional
    public void eliminar(Long id) {
        buscarAutorizada(id);
        if (favoritos.existenEnLista(id)) {
            throw new ConflictoException("La lista " + id + " todavía tiene favoritos");
        }
        if (!repository.eliminar(id)) throw noEncontrada(id);
    }

    @Transactional
    public void moverFavoritos(Long origenId, MoverFavoritosRequest request) {
        buscarAutorizada(origenId);
        buscarAutorizada(request.destinoId());
        if (origenId.equals(request.destinoId())) {
            throw new SolicitudInvalidaException("Las listas de origen y destino deben ser distintas");
        }
        favoritos.moverFavoritos(origenId, request.destinoId());
        if (!repository.eliminar(origenId)) throw noEncontrada(origenId);
    }

    public Lista buscarAutorizada(Long id) {
        var lista = repository.buscarPorId(id).orElseThrow(() -> noEncontrada(id));
        seguridad.exigirPropiedad(lista.usuarioId());
        return lista;
    }

    private RecursoNoEncontradoException noEncontrada(Long id) {
        return new RecursoNoEncontradoException("No existe la lista " + id);
    }

    private ListaResponse respuesta(Lista lista) {
        return new ListaResponse(lista.id(), lista.nombre(), lista.usuarioId());
    }
}
