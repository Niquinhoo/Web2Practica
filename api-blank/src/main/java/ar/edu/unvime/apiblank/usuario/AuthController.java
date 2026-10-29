package ar.edu.unvime.apiblank.usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import ar.edu.unvime.apiblank.seguridad.JwtService;
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Registro público y emisión de JWT Bearer")
public class AuthController {
    private final UsuarioServiceImpl usuarios;
    private final JwtService jwt;
    public AuthController(UsuarioServiceImpl usuarios, JwtService jwt) { this.usuarios = usuarios; this.jwt = jwt; }
    public record Credenciales(@NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {}
    public record UsuarioResponse(Long id, String email, Rol rol) {}
    @PostMapping("/registro")
    @Operation(summary = "Registrar usuario con rol USER")
    @ApiResponse(responseCode = "201", description = "Usuario registrado sin exponer su contraseña o hash")
    @ApiResponse(responseCode = "409", description = "Email ya registrado")
    public ResponseEntity<UsuarioResponse> registro(@Valid @RequestBody Credenciales request) {
        var usuario = usuarios.registrar(request.email(), request.password());
        return ResponseEntity.status(201).body(new UsuarioResponse(usuario.id(), usuario.email(), usuario.rol()));
    }
    @PostMapping("/login")
    @Operation(summary = "Obtener JWT usando email y contraseña")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    @ApiResponse(responseCode = "200", description = "JWT Bearer y tiempo de vigencia")
    public JwtService.TokenResponse login(@Valid @RequestBody Credenciales request) {
        return jwt.emitir(usuarios.autenticar(request.email(), request.password()));
    }
}
