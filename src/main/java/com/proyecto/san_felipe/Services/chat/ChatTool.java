package com.proyecto.san_felipe.Services.chat;

import java.util.Map;
import java.util.function.Function;

/**
 * Una herramienta que el asistente puede invocar.
 *
 * El modelo recibe {@code name}, {@code description} y {@code parameters} (JSON Schema)
 * para decidir cuando llamarla; {@code handler} es la ejecucion real contra la base de datos.
 */
public record ChatTool(
        String name,
        String description,
        Map<String, Object> parameters,
        Function<Map<String, Object>, Object> handler) {

    public Object execute(Map<String, Object> args) {
        return handler.apply(args);
    }
}
