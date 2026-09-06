package ar.edu.unvime.apiblank.producto;

import java.util.List;
import java.util.Optional;

public interface ProductoClient {
    List<ProductoExterno> listar();
    Optional<ProductoExterno> obtener(Long id);
}
