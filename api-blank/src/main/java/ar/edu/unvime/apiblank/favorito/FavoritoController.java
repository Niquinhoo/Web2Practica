package ar.edu.unvime.apiblank.favorito;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ar.edu.unvime.apiblank.error.ApiError;

@RestController
@RequestMapping("/api/favoritos")
@Tag(name = "Favoritos", description = "CRUD persistente en PostgreSQL; admite varias notas para el mismo producto")
@ApiResponse(responseCode = "400", description = "Datos o ID inválidos",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
public class FavoritoController {
    private final FavoritoService service;

    public FavoritoController(FavoritoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar favoritos", description = "Ordenados por ID ascendente.")
    public List<FavoritoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un favorito")
    @ApiResponse(responseCode = "200", description = "Favorito encontrado")
    @ApiResponse(responseCode = "404", description = "Favorito inexistente",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public FavoritoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crear un favorito", description = "Genera ID y fecha UTC. No verifica la existencia del producto externo.")
    @ApiResponse(responseCode = "201", description = "Favorito creado; Location indica su URL",
            content = @Content(schema = @Schema(implementation = FavoritoResponse.class)))
    public ResponseEntity<FavoritoResponse> crear(@Valid @RequestBody FavoritoRequest request) {
        var favorito = service.crear(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(favorito.id()).toUri();
        return ResponseEntity.created(location).body(favorito);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Reemplazar los datos de un favorito",
            description = "Reemplaza productoId, nota y listaId; conserva ID y fecha. No crea si el ID no existe.")
    @ApiResponse(responseCode = "200", description = "Favorito actualizado")
    @ApiResponse(responseCode = "404", description = "Favorito inexistente",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public FavoritoResponse actualizar(@PathVariable Long id, @Valid @RequestBody FavoritoRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un favorito")
    @ApiResponse(responseCode = "204", description = "Favorito eliminado", content = @Content)
    @ApiResponse(responseCode = "404", description = "Favorito inexistente",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
