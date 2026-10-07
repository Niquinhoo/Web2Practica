package ar.edu.unvime.apiblank.favorito.persistencia;

import java.time.Instant;
import jakarta.persistence.*;
import ar.edu.unvime.apiblank.lista.persistencia.ListaEntity;

@Entity
@Table(name = "favoritos")
public class FavoritoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "producto_id", nullable = false)
    private Long productoId;
    @Column(length = 500)
    private String nota;
    @Column(name = "fecha_alta", nullable = false)
    private Instant fechaAgregado;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lista_id", nullable = false)
    private ListaEntity lista;

    protected FavoritoEntity() {}

    public FavoritoEntity(Long productoId, String nota, Instant fechaAgregado, ListaEntity lista) {
        this.productoId = productoId;
        this.nota = nota;
        // PostgreSQL conserva microsegundos: POST y GET deben devolver la misma fecha.
        this.fechaAgregado = fechaAgregado.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.lista = lista;
    }

    public void actualizar(Long productoId, String nota, ListaEntity lista) {
        this.productoId = productoId;
        this.nota = nota;
        this.lista = lista;
    }

    public Long getId() { return id; }
    public Long getProductoId() { return productoId; }
    public String getNota() { return nota; }
    public Instant getFechaAgregado() { return fechaAgregado; }
    public Long getListaId() { return lista.getId(); }
}
