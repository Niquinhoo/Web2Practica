package ar.edu.unvime.apiblank.producto;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ar.edu.unvime.apiblank.error.ApiError;

@RestController
@RequestMapping("/api/productos")
@Tag(name = "Productos", description = "Catálogo externo de solo lectura")
@ApiResponse(responseCode = "502", description = "Proveedor no disponible o respuesta inválida",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
@ApiResponse(responseCode = "400", description = "ID inválido",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
public class ProductoController {
    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar productos", description = "Consulta todo el catálogo de DummyJSON y devuelve un DTO propio, sin paginación.")
    @ApiResponse(responseCode = "200", description = "Catálogo obtenido")
    public List<ProductoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un producto", description = "Consulta un producto externo por su ID.")
    @ApiResponse(responseCode = "200", description = "Producto encontrado")
    @ApiResponse(responseCode = "404", description = "Producto inexistente",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ProductoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }
}
