package ar.edu.unvime.apiblank.usuario;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ar.edu.unvime.apiblank.error.*;
@Service
@Transactional(readOnly = true)
public class UsuarioServiceImpl {
    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final String hashInexistente;
    public UsuarioServiceImpl(UsuarioRepository repository, PasswordEncoder encoder) {
        this.repository = repository; this.encoder = encoder;
        this.hashInexistente = encoder.encode("cuenta-inexistente");
    }
    @Transactional
    public Usuario registrar(String email, String password) {
        validarPassword(password);
        email = normalizar(email);
        if (repository.buscarPorEmail(email).isPresent()) throw new ConflictoException("El email ya está registrado");
        return repository.crear(email, encoder.encode(password), Rol.USER);
    }
    public Usuario autenticar(String email, String password) {
        validarPassword(password);
        var usuario = repository.buscarPorEmail(normalizar(email));
        boolean coincide = encoder.matches(password, usuario.map(Usuario::passwordHash).orElse(hashInexistente));
        if (usuario.isEmpty() || !coincide) throw new BadCredentialsException("Credenciales inválidas");
        return usuario.get();
    }
    private String normalizar(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private void validarPassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new SolicitudInvalidaException("La contraseña no debe superar 72 bytes UTF-8");
    }
}
