package ar.edu.unvime.apiblank.lista.persistencia;

import jakarta.persistence.*;

@Entity
@Table(name = "listas")
public class ListaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private ar.edu.unvime.apiblank.usuario.persistencia.UsuarioEntity usuario;

    protected ListaEntity() {}

    public ListaEntity(String nombre, ar.edu.unvime.apiblank.usuario.persistencia.UsuarioEntity usuario) {
        this.nombre = nombre;
        this.usuario = usuario;
    }

    public Long getUsuarioId() { return usuario == null ? null : usuario.getId(); }
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
}
