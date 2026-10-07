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

    protected ListaEntity() {}

    public ListaEntity(String nombre) {
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
}
