package ar.edu.unvime.apiblank.producto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DummyJsonProductosResponse(List<ProductoExterno> products) {}
