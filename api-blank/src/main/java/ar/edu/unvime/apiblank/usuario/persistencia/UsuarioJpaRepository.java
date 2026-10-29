package ar.edu.unvime.apiblank.usuario.persistencia;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {
    Optional<UsuarioEntity> findByEmail(String email);
}
