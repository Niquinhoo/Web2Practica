package ar.edu.unvime.apiblank.usuario.persistencia;
import jakarta.persistence.*;
import ar.edu.unvime.apiblank.usuario.Rol;
@Entity
@Table(name = "usuarios")
public class UsuarioEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private Rol rol;
    protected UsuarioEntity() {}
    public UsuarioEntity(String email, String passwordHash, Rol rol) {
        this.email = email; this.passwordHash = passwordHash; this.rol = rol;
    }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Rol getRol() { return rol; }
}
