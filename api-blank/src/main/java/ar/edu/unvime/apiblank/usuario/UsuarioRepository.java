package ar.edu.unvime.apiblank.usuario;
import java.util.Optional;
public interface UsuarioRepository {
    Optional<Usuario> buscarPorEmail(String email);
    Usuario crear(String email, String passwordHash, Rol rol);
}
