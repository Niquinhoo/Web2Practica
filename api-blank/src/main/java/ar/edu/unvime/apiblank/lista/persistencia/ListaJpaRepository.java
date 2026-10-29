package ar.edu.unvime.apiblank.lista.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ListaJpaRepository extends JpaRepository<ListaEntity, Long> {
    java.util.List<ListaEntity> findByUsuario_IdOrderByIdAsc(Long usuarioId);
}
