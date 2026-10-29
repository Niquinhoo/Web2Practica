package ar.edu.unvime.apiblank.usuario.persistencia;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ar.edu.unvime.apiblank.usuario.*;
@Repository
@Transactional(readOnly = true)
public class UsuarioRepositoryAdapter implements UsuarioRepository {
    private final UsuarioJpaRepository repository;
    public UsuarioRepositoryAdapter(UsuarioJpaRepository repository) { this.repository = repository; }
    public Optional<Usuario> buscarPorEmail(String email) { return repository.findByEmail(email).map(this::dominio); }
    @Transactional
    public Usuario crear(String email, String hash, Rol rol) {
        return dominio(repository.saveAndFlush(new UsuarioEntity(email, hash, rol)));
    }
    private Usuario dominio(UsuarioEntity entity) {
        return new Usuario(entity.getId(), entity.getEmail(), entity.getPasswordHash(), entity.getRol());
    }
}
