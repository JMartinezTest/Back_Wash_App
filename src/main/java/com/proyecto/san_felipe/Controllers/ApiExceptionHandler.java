package com.proyecto.san_felipe.Controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Traduce las excepciones de los servicios a respuestas con mensaje.
 *
 * Sin esto, Spring Security responde 403 con el cuerpo vacio ante cualquier
 * excepcion no controlada y el frontend no puede explicar que fallo.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Datos invalidos o reglas de negocio incumplidas. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", mensaje(e)));
    }

    /** Los servicios lanzan RuntimeException cuando no encuentran el recurso. */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(RuntimeException e) {
        String texto = mensaje(e);
        boolean esNoEncontrado = texto.toLowerCase().contains("no existe")
                || texto.toLowerCase().contains("not found");
        HttpStatus estado = esNoEncontrado ? HttpStatus.NOT_FOUND : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(estado).body(Map.of("error", texto));
    }

    private String mensaje(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
