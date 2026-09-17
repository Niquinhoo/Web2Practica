package ar.edu.unvime.apiblank.error;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> conflicto(org.springframework.dao.DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(new ApiError(409,
                "Conflicto de integridad: no se puede borrar una lista con favoritos ni referenciar una lista eliminada",
                Map.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> solicitudInvalida(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ApiError(400, ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> noEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(404).body(new ApiError(404, ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(ServicioExternoException.class)
    public ResponseEntity<ApiError> servicioExterno(ServicioExternoException ex) {
        log.warn("Falló la consulta al catálogo externo", ex);
        return ResponseEntity.status(502).body(new ApiError(502,
                "No se pudo consultar el catálogo externo. Intentá nuevamente más tarde.", Map.of()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return new ResponseEntity<>(new ApiError(400, "Hay datos inválidos", campos), headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String mensaje = switch (status.value()) {
            case 400 -> "Solicitud inválida: revisá el JSON, los parámetros y los tipos de datos";
            case 404 -> "No existe el recurso solicitado";
            case 405 -> "Método HTTP no permitido";
            case 406 -> "Formato de respuesta no aceptable";
            case 415 -> "Tipo de contenido no soportado; utilizá application/json";
            default -> status.is5xxServerError() ? "Error interno del servidor" : "Solicitud inválida";
        };
        return new ResponseEntity<>(new ApiError(status.value(), mensaje, Map.of()), headers, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> inesperado(Exception ex) {
        log.error("Error inesperado al procesar la solicitud", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(500, "Error interno del servidor", Map.of()));
    }
}
