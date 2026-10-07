package ar.edu.unvime.apiblank.lista.persistencia;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ar.edu.unvime.apiblank.lista.Lista;
import ar.edu.unvime.apiblank.lista.ListaRepository;

@Repository
@Transactional(readOnly = true)
public class ListaRepositoryAdapter implements ListaRepository {
    private final ListaJpaRepository repository;

    public ListaRepositoryAdapter(ListaJpaRepository repository) {
        this.repository = repository;
    }

    public List<Lista> buscarTodos() {
        return repository.findAll(Sort.by("id")).stream().map(this::dominio).toList();
    }

    public Optional<Lista> buscarPorId(Long id) {
        return repository.findById(id).map(this::dominio);
    }

    @Transactional
    public Lista crear(String nombre) {
        return dominio(repository.save(new ListaEntity(nombre)));
    }

    @Transactional
    public boolean eliminar(Long id) {
        var lista = repository.findById(id);
        if (lista.isEmpty()) return false;
        repository.delete(lista.get());
        // Fuerza la comprobación de la FK dentro de la transacción.
        repository.flush();
        return true;
    }

    private Lista dominio(ListaEntity entity) {
        return new Lista(entity.getId(), entity.getNombre());
    }
}
