package ar.edu.unvime.apiblank.producto;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ar.edu.unvime.apiblank.error.ServicioExternoException;

@Component
public class DummyJsonProductoClient implements ProductoClient {
    private final RestClient restClient;

    public DummyJsonProductoClient(RestClient dummyJsonRestClient) {
        this.restClient = dummyJsonRestClient;
    }

    @Override
    public List<ProductoExterno> listar() {
        try {
            var respuesta = restClient.get().uri("/products?limit=0").retrieve()
                    .body(DummyJsonProductosResponse.class);
            if (respuesta == null || respuesta.products() == null) {
                throw new ServicioExternoException("El proveedor devolvió un catálogo vacío o inválido");
            }
            respuesta.products().forEach(this::validar);
            return List.copyOf(respuesta.products());
        } catch (RestClientException ex) {
            throw new ServicioExternoException("Falló la consulta de productos", ex);
        }
    }

    @Override
    public Optional<ProductoExterno> obtener(Long id) {
        try {
            var producto = restClient.get().uri("/products/{id}", id).retrieve().body(ProductoExterno.class);
            validar(producto);
            if (!id.equals(producto.id())) {
                throw new ServicioExternoException("El proveedor devolvió un producto diferente");
            }
            return Optional.of(producto);
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (RestClientException ex) {
            throw new ServicioExternoException("Falló la consulta del producto", ex);
        }
    }

    private void validar(ProductoExterno producto) {
        if (producto == null || producto.id() == null || producto.id() <= 0
                || producto.title() == null || producto.title().isBlank()
                || producto.price() == null || producto.price().signum() < 0
                || producto.description() == null || producto.category() == null) {
            throw new ServicioExternoException("El proveedor devolvió un producto inválido");
        }
    }
}
