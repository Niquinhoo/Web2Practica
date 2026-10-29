package ar.edu.unvime.apiblank.seguridad;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecretKey jwtKey(@Value("${jwt.secreto}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalArgumentException("JWT_SECRETO requiere al menos 32 bytes");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
    @Bean JwtDecoder jwtDecoder(SecretKey key) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> identidad = jwt -> {
            try {
                if (Long.parseLong(jwt.getSubject()) > 0 && java.util.Set.of("USER", "ADMIN").contains(jwt.getClaimAsString("rol")))
                    return OAuth2TokenValidatorResult.success();
            } catch (RuntimeException ignored) { }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer("web2-practica"), identidad));
        return decoder;
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("rol"); authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter(); converter.setJwtGrantedAuthoritiesConverter(authorities);
        return http
                // El bearer se envía explícitamente en Authorization; no usamos cookies de autenticación.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/registro", "/api/auth/login").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/productos/**", "/health", "/ping", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                    .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                    .authenticationEntryPoint((req, res, ex) -> error(res, 401, "Se requiere un token válido"))
                    .accessDeniedHandler((req, res, ex) -> error(res, 403, "Acceso denegado")))
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                    .authenticationEntryPoint((req, res, ex) -> error(res, 401, "Se requiere un token válido")))
                .build();
    }
    private void error(HttpServletResponse response, int status, String mensaje) throws IOException {
        response.setStatus(status); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        response.getWriter().write("{\"status\":" + status + ",\"mensaje\":\"" + mensaje + "\",\"campos\":{}}");
    }
}
