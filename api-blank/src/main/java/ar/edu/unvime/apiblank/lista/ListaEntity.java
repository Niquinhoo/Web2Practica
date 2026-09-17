package ar.edu.unvime.apiblank.lista;

import jakarta.persistence.*;

@Entity
@Table(name = "listas")
public class ListaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 255)
    private String nombre;

    protected ListaEntity() {}

    ListaEntity(String nombre) {
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    Lista toDomain() {
        return new Lista(id, nombre);
    }
}
