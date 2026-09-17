package ar.edu.unvime.apiblank.lista;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class ListaRepositoryAdapter implements ListaRepository {
    private final ListaJpaRepository repository;

    public ListaRepositoryAdapter(ListaJpaRepository repository) {
        this.repository = repository;
    }

    public List<Lista> buscarTodos() {
        return repository.findAll(Sort.by("id")).stream().map(ListaEntity::toDomain).toList();
    }

    public Optional<Lista> buscarPorId(Long id) {
        return repository.findById(id).map(ListaEntity::toDomain);
    }

    @Transactional
    public Lista crear(String nombre) {
        return repository.save(new ListaEntity(nombre)).toDomain();
    }

    @Transactional
    public boolean eliminar(Long id) {
        return repository.findById(id).map(entity -> {
            repository.delete(entity);
            repository.flush(); // La FK impide borrar listas con favoritos, incluso ante concurrencia.
            return true;
        }).orElse(false);
    }
}
