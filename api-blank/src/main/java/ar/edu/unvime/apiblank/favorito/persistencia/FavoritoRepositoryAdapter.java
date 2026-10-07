package ar.edu.unvime.apiblank.favorito.persistencia;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ar.edu.unvime.apiblank.favorito.Favorito;
import ar.edu.unvime.apiblank.favorito.FavoritoRepository;
import ar.edu.unvime.apiblank.lista.persistencia.ListaJpaRepository;

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
        return repository.findAll(Sort.by("id")).stream().map(this::dominio).toList();
    }

    public Optional<Favorito> buscarPorId(Long id) {
        return repository.findById(id).map(this::dominio);
    }

    public List<Favorito> buscarPorListaId(Long listaId) {
        return repository.findByLista_IdOrderByIdAsc(listaId).stream().map(this::dominio).toList();
    }

    public boolean existenEnLista(Long listaId) {
        return repository.existsByLista_Id(listaId);
    }

    @Transactional
    public Favorito crear(Long productoId, String nota, Instant fechaAgregado, Long listaId) {
        return dominio(repository.save(new FavoritoEntity(productoId, nota, fechaAgregado,
                listas.getReferenceById(listaId))));
    }

    @Transactional
    public Optional<Favorito> actualizar(Long id, Long productoId, String nota, Long listaId) {
        return repository.findById(id).map(entity -> {
            entity.actualizar(productoId, nota, listas.getReferenceById(listaId));
            repository.flush();
            return dominio(entity);
        });
    }

    @Transactional
    public boolean eliminar(Long id) {
        var favorito = repository.findById(id);
        if (favorito.isEmpty()) return false;
        repository.delete(favorito.get());
        repository.flush();
        return true;
    }

    @Transactional
    public int moverFavoritos(Long origenId, Long destinoId) {
        return repository.moverFavoritos(origenId, listas.getReferenceById(destinoId));
    }

    private Favorito dominio(FavoritoEntity entity) {
        return new Favorito(entity.getId(), entity.getProductoId(), entity.getNota(),
                entity.getFechaAgregado(), entity.getListaId());
    }
}
