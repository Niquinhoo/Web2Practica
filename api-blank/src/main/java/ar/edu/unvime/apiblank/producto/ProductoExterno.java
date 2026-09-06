package ar.edu.unvime.apiblank.producto;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductoExterno(Long id, String title, String description, BigDecimal price, String category) {}
