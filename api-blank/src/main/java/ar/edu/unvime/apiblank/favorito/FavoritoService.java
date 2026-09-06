package ar.edu.unvime.apiblank.favorito;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import ar.edu.unvime.apiblank.error.RecursoNoEncontradoException;

@Service
public class FavoritoService {
    private final FavoritoRepository repository;
    private final Clock clock;

    public FavoritoService(FavoritoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<FavoritoResponse> listar() {
        return repository.buscarTodos().stream().map(this::respuesta).toList();
    }

    public FavoritoResponse obtener(Long id) {
        return respuesta(repository.buscarPorId(id).orElseThrow(() -> noEncontrado(id)));
    }

    public FavoritoResponse crear(FavoritoRequest request) {
        return respuesta(repository.crear(request.productoId(), request.nota(), Instant.now(clock)));
    }

    public FavoritoResponse actualizar(Long id, FavoritoRequest request) {
        return respuesta(repository.actualizar(id, request.productoId(), request.nota())
                .orElseThrow(() -> noEncontrado(id)));
    }

    public void eliminar(Long id) {
        if (!repository.eliminar(id)) {
            throw noEncontrado(id);
        }
    }

    private RecursoNoEncontradoException noEncontrado(Long id) {
        return new RecursoNoEncontradoException("No existe el favorito " + id);
    }

    private FavoritoResponse respuesta(Favorito favorito) {
        return new FavoritoResponse(favorito.id(), favorito.productoId(),
                favorito.nota(), favorito.fechaAgregado());
    }
}
