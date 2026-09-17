package ar.edu.unvime.apiblank.favorito;

import java.time.Instant;
import jakarta.persistence.*;
import ar.edu.unvime.apiblank.lista.ListaEntity;

@Entity
@Table(name = "favoritos")
public class FavoritoEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "producto_id", nullable = false)
    Long productoId;
    @Column(length = 500)
    String nota;
    @Column(name = "fecha_alta", nullable = false)
    Instant fechaAgregado;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lista_id", nullable = false)
    ListaEntity lista;

    protected FavoritoEntity() {}

    Favorito toDomain() {
        return new Favorito(id, productoId, nota, fechaAgregado, lista.getId());
    }
}
