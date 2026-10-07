package ar.edu.unvime.apiblank.lista;

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
import ar.edu.unvime.apiblank.favorito.FavoritoResponse;

@RestController
@RequestMapping("/api/listas")
@Tag(name = "Listas", description = "Organización persistente de favoritos")
@ApiResponse(responseCode = "400", description = "Datos inválidos",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
@ApiResponse(responseCode = "404", description = "Lista inexistente",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
public class ListaController {
    private final ListaService service;

    public ListaController(ListaService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Crear una lista", description = "Nombre obligatorio de hasta 100 caracteres.")
    @ApiResponse(responseCode = "201", description = "Lista creada; Location indica su URL",
            content = @Content(schema = @Schema(implementation = ListaResponse.class)))
    public ResponseEntity<ListaResponse> crear(@Valid @RequestBody ListaRequest request) {
        var lista = service.crear(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(lista.id()).toUri();
        return ResponseEntity.created(location).body(lista);
    }

    @GetMapping
    @Operation(summary = "Listar listas", description = "Devuelve las listas ordenadas por ID ascendente.")
    public List<ListaResponse> listar() { return service.listar(); }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una lista", description = "Devuelve ID y nombre de una lista existente.")
    public ListaResponse obtener(@PathVariable Long id) { return service.obtener(id); }

    @GetMapping("/{id}/favoritos")
    @Operation(summary = "Listar favoritos de una lista", description = "Lista vacía: array vacío. Lista inexistente: 404.")
    public List<FavoritoResponse> favoritos(@PathVariable Long id) { return service.favoritos(id); }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una lista vacía", description = "Si contiene favoritos devuelve 409 Conflict.")
    @ApiResponse(responseCode = "204", description = "Lista eliminada", content = @Content)
    @ApiResponse(responseCode = "409", description = "La lista todavía tiene favoritos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{origenId}/mover-favoritos")
    @Operation(summary = "Mover favoritos y eliminar origen",
            description = "Reasigna todos los favoritos a destinoId y elimina el origen en una única transacción. Origen y destino deben ser distintos.")
    @ApiResponse(responseCode = "204", description = "Favoritos movidos y origen eliminado", content = @Content)
    @ApiResponse(responseCode = "409", description = "Conflicto de integridad; ninguna escritura queda aplicada",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<Void> mover(@PathVariable Long origenId, @Valid @RequestBody MoverFavoritosRequest request) {
        service.moverFavoritos(origenId, request);
        return ResponseEntity.noContent().build();
    }
}
