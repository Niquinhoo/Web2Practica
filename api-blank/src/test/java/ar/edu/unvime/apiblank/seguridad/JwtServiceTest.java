package ar.edu.unvime.apiblank.seguridad;
import static org.assertj.core.api.Assertions.*;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import ar.edu.unvime.apiblank.usuario.*;
class JwtServiceTest {
    final SecurityConfig config = new SecurityConfig();
    final javax.crypto.SecretKey key = config.jwtKey("clave-de-prueba-larga-con-al-menos-32-bytes");
    final JwtEncoder encoder = config.jwtEncoder(key);
    final JwtDecoder decoder = config.jwtDecoder(key);
    @Test void emitir_usuario_incluyeIdentidadRolYVencimiento() {
        var token = new JwtService(encoder,Clock.systemUTC(),30).emitir(new Usuario(7L,"u@example.com","hash-no-publico",Rol.USER));
        var jwt = decoder.decode(token.token());
        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("rol")).isEqualTo("USER");
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
        assertThat(jwt.getClaims()).doesNotContainKeys("password","passwordHash");
        assertThat(token.expiresIn()).isEqualTo(1800);
    }
    @ParameterizedTest @ValueSource(strings={"0", "-1", "texto"})
    void validar_subjectInvalido_rechazaTokenFirmado(String subject) {
        var claims=JwtClaimsSet.builder().issuer("web2-practica").subject(subject).claim("rol","USER")
                .expiresAt(Instant.now().plusSeconds(300)).build();
        String token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        assertThatThrownBy(()->decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }
    @Test void configurar_claveCortaOVigenciaInvalida_falla() {
        assertThatThrownBy(()->config.jwtKey("corta")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new JwtService(encoder,Clock.systemUTC(),0)).isInstanceOf(IllegalArgumentException.class);
    }
}
