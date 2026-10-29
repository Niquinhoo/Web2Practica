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

    private final ar.edu.unvime.apiblank.usuario.persistencia.UsuarioJpaRepository usuarios;

    public ListaRepositoryAdapter(ListaJpaRepository repository, ar.edu.unvime.apiblank.usuario.persistencia.UsuarioJpaRepository usuarios) {
        this.repository = repository;
        this.usuarios = usuarios;
    }

    public List<Lista> buscarTodos() {
        return repository.findAll(Sort.by("id")).stream().map(this::dominio).toList();
    }

    public Optional<Lista> buscarPorId(Long id) {
        return repository.findById(id).map(this::dominio);
    }

    public List<Lista> buscarPorUsuarioId(Long usuarioId) {
        return repository.findByUsuario_IdOrderByIdAsc(usuarioId).stream().map(this::dominio).toList();
    }

    @Transactional
    public Lista crear(String nombre, Long usuarioId) {
        return dominio(repository.save(new ListaEntity(nombre, usuarios.getReferenceById(usuarioId))));
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
        return new Lista(entity.getId(), entity.getNombre(), entity.getUsuarioId());
    }
}
