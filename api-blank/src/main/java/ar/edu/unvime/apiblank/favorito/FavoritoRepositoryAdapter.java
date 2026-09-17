package ar.edu.unvime.apiblank.favorito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ar.edu.unvime.apiblank.lista.ListaJpaRepository;

@Repository
@Transactional(readOnly = true)
public class FavoritoRepositoryAdapter implements FavoritoRepository {
    private final FavoritoJpaRepository repository;
    private final ListaJpaRepository listas;

    public FavoritoRepositoryAdapter(FavoritoJpaRepository repository, ListaJpaRepository listas) {
        this.repository = repository;
        this.listas = listas;
    }

    public List<Favorito> buscarTodos() {
        return repository.findAll(Sort.by("id")).stream().map(FavoritoEntity::toDomain).toList();
    }

    public Optional<Favorito> buscarPorId(Long id) {
        return repository.findById(id).map(FavoritoEntity::toDomain);
    }

    @Transactional
    public Favorito crear(Long productoId, String nota, Instant fechaAgregado, Long listaId) {
        var entity = new FavoritoEntity();
        entity.productoId = productoId;
        entity.nota = nota;
        // PostgreSQL almacena microsegundos: POST y GET deben devolver la misma fecha.
        entity.fechaAgregado = fechaAgregado.truncatedTo(ChronoUnit.MICROS);
        entity.lista = listas.getReferenceById(listaId);
        return repository.save(entity).toDomain();
    }

    @Transactional
    public Optional<Favorito> actualizar(Long id, Long productoId, String nota, Long listaId) {
        return repository.findById(id).map(entity -> {
            entity.productoId = productoId;
            entity.nota = nota;
            entity.lista = listas.getReferenceById(listaId);
            return entity.toDomain();
        });
    }

    @Transactional
    public boolean eliminar(Long id) {
        return repository.findById(id).map(entity -> {
            repository.delete(entity);
            return true;
        }).orElse(false);
    }

    public List<Favorito> buscarPorListaId(Long listaId) {
        return repository.findByListaIdOrderByIdAsc(listaId).stream().map(FavoritoEntity::toDomain).toList();
    }

    @Transactional
    public void moverDeLista(Long origenId, Long destinoId) {
        repository.moverDeLista(origenId, listas.getReferenceById(destinoId));
    }
}
