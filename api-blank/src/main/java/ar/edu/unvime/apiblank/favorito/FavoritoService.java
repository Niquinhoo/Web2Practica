package ar.edu.unvime.apiblank.favorito;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ar.edu.unvime.apiblank.lista.ListaRepository;
import ar.edu.unvime.apiblank.error.RecursoNoEncontradoException;

@Service
@Transactional(readOnly = true)
public class FavoritoService {
    private final FavoritoRepository repository;
    private final Clock clock;
    private final ListaRepository listas;

    public FavoritoService(FavoritoRepository repository, Clock clock, ListaRepository listas) {
        this.repository = repository;
        this.clock = clock;
        this.listas = listas;
    }

    public List<FavoritoResponse> listar() {
        return repository.buscarTodos().stream().map(this::respuesta).toList();
    }

    public FavoritoResponse obtener(Long id) {
        return respuesta(repository.buscarPorId(id).orElseThrow(() -> noEncontrado(id)));
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
        listas.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe la lista " + id));
    }
}
