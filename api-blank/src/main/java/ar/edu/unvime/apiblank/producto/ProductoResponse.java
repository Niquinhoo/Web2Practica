package ar.edu.unvime.apiblank.producto;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductoResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Essence Mascara Lash Princess") String nombre,
        @Schema(example = "Máscara de pestañas") String descripcion,
        @Schema(example = "9.99") BigDecimal precio,
        @Schema(example = "beauty") String categoria) {}
