package ar.edu.unvime.apiblank.favorito;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;

public record FavoritoResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "1") Long productoId,
        @Schema(example = "Comprar para regalar") String nota,
        @Schema(description = "Fecha UTC generada al crear; se conserva al actualizar",
                example = "2026-09-06T12:00:00Z") Instant fechaAgregado) {}
