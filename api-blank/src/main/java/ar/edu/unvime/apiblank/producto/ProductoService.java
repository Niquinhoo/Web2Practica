package ar.edu.unvime.apiblank.producto;

import java.util.List;
import org.springframework.stereotype.Service;
import ar.edu.unvime.apiblank.error.RecursoNoEncontradoException;

@Service
public class ProductoService {
    private final ProductoClient client;

    public ProductoService(ProductoClient client) {
        this.client = client;
    }

    public List<ProductoResponse> listar() {
        return client.listar().stream().map(this::respuesta).toList();
    }

    public ProductoResponse obtener(Long id) {
        return respuesta(client.obtener(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + id)));
    }

    private ProductoResponse respuesta(ProductoExterno producto) {
        return new ProductoResponse(producto.id(), producto.title(), producto.description(),
                producto.price(), producto.category());
    }
}
