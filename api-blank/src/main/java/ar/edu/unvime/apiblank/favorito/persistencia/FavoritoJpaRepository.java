package ar.edu.unvime.apiblank.favorito.persistencia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoritoJpaRepository extends JpaRepository<FavoritoEntity, Long> {
    List<FavoritoEntity> findByLista_IdOrderByIdAsc(Long listaId);
    boolean existsByLista_Id(Long listaId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update FavoritoEntity f set f.lista = :destino where f.lista.id = :origenId")
    int moverFavoritos(@Param("origenId") Long origenId,
            @Param("destino") ar.edu.unvime.apiblank.lista.persistencia.ListaEntity destino);
}
