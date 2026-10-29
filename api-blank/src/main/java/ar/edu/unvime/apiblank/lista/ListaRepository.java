package ar.edu.unvime.apiblank.lista;

import java.util.List;
import java.util.Optional;

public interface ListaRepository {
    List<Lista> buscarTodos();
    Optional<Lista> buscarPorId(Long id);
    List<Lista> buscarPorUsuarioId(Long usuarioId);
    Lista crear(String nombre, Long usuarioId);
    boolean eliminar(Long id);
}
