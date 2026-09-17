package ar.edu.unvime.apiblank.lista;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ListaRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 255, message = "no debe superar los 255 caracteres") String nombre) {}
