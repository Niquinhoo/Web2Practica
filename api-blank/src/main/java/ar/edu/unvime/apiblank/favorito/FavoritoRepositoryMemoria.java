package ar.edu.unvime.apiblank.favorito;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class FavoritoRepositoryMemoria implements FavoritoRepository {
    private final ConcurrentHashMap<Long, Favorito> favoritos = new ConcurrentHashMap<>();
    private final AtomicLong secuencia = new AtomicLong();

    @Override
    public List<Favorito> buscarTodos() {
        return favoritos.values().stream().sorted(Comparator.comparing(Favorito::id)).toList();
    }

    @Override
    public Optional<Favorito> buscarPorId(Long id) {
        return Optional.ofNullable(favoritos.get(id));
    }

    @Override
    public Favorito crear(Long productoId, String nota, Instant fechaAgregado) {
        var favorito = new Favorito(secuencia.incrementAndGet(), productoId, nota, fechaAgregado);
        favoritos.put(favorito.id(), favorito);
        return favorito;
    }

    @Override
    public Optional<Favorito> actualizar(Long id, Long productoId, String nota) {
        // La actualización atómica evita recrear un favorito eliminado por otra solicitud.
        return Optional.ofNullable(favoritos.computeIfPresent(id,
                (clave, anterior) -> new Favorito(clave, productoId, nota, anterior.fechaAgregado())));
    }

    @Override
    public boolean eliminar(Long id) {
        return favoritos.remove(id) != null;
    }
}
