package ar.edu.unvime.apiblank.usuario;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import ar.edu.unvime.apiblank.error.*;
class UsuarioServiceImplTest {
    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UsuarioServiceImpl service = new UsuarioServiceImpl(repository, encoder);
    @Test void registrar_password_guardaSoloHashConRolUser() {
        when(repository.crear(eq("user@example.com"), anyString(), eq(Rol.USER)))
                .thenAnswer(inv -> new Usuario(1L, inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)));
        var usuario = service.registrar("USER@example.com", "Una-clave-123");
        assertThat(usuario.passwordHash()).isNotEqualTo("Una-clave-123");
        assertThat(encoder.matches("Una-clave-123", usuario.passwordHash())).isTrue();
        assertThat(usuario.rol()).isEqualTo(Rol.USER);
    }
    @Test void registrar_emailRepetido_noEscribe() {
        when(repository.buscarPorEmail("user@example.com")).thenReturn(Optional.of(new Usuario(1L,"user@example.com","hash",Rol.USER)));
        assertThatThrownBy(() -> service.registrar("USER@example.com", "Una-clave-123")).isInstanceOf(ConflictoException.class);
        verify(repository, never()).crear(anyString(), anyString(), any());
    }
    @Test void autenticar_inexistenteOClaveErronea_mismoError() {
        assertThatThrownBy(() -> service.autenticar("nadie@example.com", "Una-clave-123")).isInstanceOf(BadCredentialsException.class);
        when(repository.buscarPorEmail("user@example.com")).thenReturn(Optional.of(new Usuario(1L,"user@example.com",encoder.encode("otra-clave"),Rol.USER)));
        assertThatThrownBy(() -> service.autenticar("user@example.com", "Una-clave-123")).isInstanceOf(BadCredentialsException.class);
    }
    @Test void registrar_passwordUnicodeDemasiadoLarga_rechazaSinEscribir() {
        assertThatThrownBy(() -> service.registrar("user@example.com", "á".repeat(40))).isInstanceOf(SolicitudInvalidaException.class);
        verifyNoInteractions(repository);
    }
}
