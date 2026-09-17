package ar.edu.unvime.apiblank.favorito;

import java.util.List;
import org.springframework.data.jpa.repository.*;
import ar.edu.unvime.apiblank.lista.ListaEntity;

public interface FavoritoJpaRepository extends JpaRepository<FavoritoEntity, Long> {
    List<FavoritoEntity> findByListaIdOrderByIdAsc(Long listaId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update FavoritoEntity f set f.lista = :destino where f.lista.id = :origenId")
    int moverDeLista(Long origenId, ListaEntity destino);
}
