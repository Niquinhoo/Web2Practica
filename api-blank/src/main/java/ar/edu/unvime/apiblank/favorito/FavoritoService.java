package ar.edu.unvime.apiblank.favorito;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ar.edu.unvime.apiblank.lista.ListaServiceImpl;
import ar.edu.unvime.apiblank.seguridad.ContextoSeguridad;
import ar.edu.unvime.apiblank.error.RecursoNoEncontradoException;

@Service
@Transactional(readOnly = true)
public class FavoritoService {
    private final FavoritoRepository repository;
    private final Clock clock;
    private final ListaServiceImpl listas;

    private final ContextoSeguridad seguridad;

    public FavoritoService(FavoritoRepository repository, Clock clock, ListaServiceImpl listas, ContextoSeguridad seguridad) {
        this.repository = repository;
        this.clock = clock;
        this.listas = listas;
        this.seguridad = seguridad;
    }

    public List<FavoritoResponse> listar() {
        return (seguridad.esAdmin() ? repository.buscarTodos() : repository.buscarPorUsuarioId(seguridad.usuarioId())).stream().map(this::respuesta).toList();
    }

    public FavoritoResponse obtener(Long id) {
        var favorito = repository.buscarPorId(id).orElseThrow(() -> noEncontrado(id));
        validarLista(favorito.listaId());
        return respuesta(favorito);
    }

    @Transactional
    public FavoritoResponse crear(FavoritoRequest request) {
        validarLista(request.listaId());
        return respuesta(repository.crear(request.productoId(), request.nota(), Instant.now(clock), request.listaId()));
    }

    @Transactional
    public FavoritoResponse actualizar(Long id, FavoritoRequest request) {
        obtener(id);
        validarLista(request.listaId());
        return respuesta(repository.actualizar(id, request.productoId(), request.nota(), request.listaId())
                .orElseThrow(() -> noEncontrado(id)));
    }

    @Transactional
    public void eliminar(Long id) {
        obtener(id);
        if (!repository.eliminar(id)) {
            throw noEncontrado(id);
        }
    }

    private RecursoNoEncontradoException noEncontrado(Long id) {
        return new RecursoNoEncontradoException("No existe el favorito " + id);
    }

    private FavoritoResponse respuesta(Favorito favorito) {
        return FavoritoResponse.desde(favorito);
    }

    private void validarLista(Long id) {
        listas.buscarAutorizada(id);
    }
}
