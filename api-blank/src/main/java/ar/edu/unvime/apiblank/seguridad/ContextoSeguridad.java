package ar.edu.unvime.apiblank.seguridad;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
@Component
public class ContextoSeguridad {
    private Jwt identidad() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Jwt jwt))
            throw new AuthenticationCredentialsNotFoundException("Se requiere autenticación");
        return jwt;
    }
    public Long usuarioId() { return Long.valueOf(identidad().getSubject()); }
    public boolean esAdmin() { return "ADMIN".equals(identidad().getClaimAsString("rol")); }
    public void exigirPropiedad(Long usuarioId) {
        if (!esAdmin() && !Objects.equals(usuarioId(), usuarioId))
            throw new AccessDeniedException("El recurso pertenece a otro usuario");
    }
}
