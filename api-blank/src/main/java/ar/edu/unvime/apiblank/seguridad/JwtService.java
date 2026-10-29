package ar.edu.unvime.apiblank.seguridad;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import ar.edu.unvime.apiblank.usuario.Usuario;
@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final Clock clock;
    private final long minutos;
    public JwtService(JwtEncoder encoder, Clock clock, @Value("${jwt.vigencia-minutos}") long minutos) {
        if (minutos <= 0) throw new IllegalArgumentException("La vigencia JWT debe ser positiva");
        this.encoder = encoder; this.clock = clock; this.minutos = minutos;
    }
    public TokenResponse emitir(Usuario usuario) {
        var ahora = clock.instant();
        var claims = JwtClaimsSet.builder().issuer("web2-practica").subject(usuario.id().toString())
                .claim("rol", usuario.rol().name()).issuedAt(ahora).expiresAt(ahora.plusSeconds(minutos * 60)).build();
        var token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims));
        return new TokenResponse(token.getTokenValue(), "Bearer", minutos * 60);
    }
    public record TokenResponse(String token, String tokenType, long expiresIn) {}
}
