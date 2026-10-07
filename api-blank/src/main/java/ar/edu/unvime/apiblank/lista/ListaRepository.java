package ar.edu.unvime.apiblank.lista;

import java.util.List;
import java.util.Optional;

public interface ListaRepository {
    List<Lista> buscarTodos();
    Optional<Lista> buscarPorId(Long id);
    Lista crear(String nombre);
    boolean eliminar(Long id);
}
