package com.proyecto.san_felipe.Services.chat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Ayudas para declarar el JSON Schema de una herramienta y leer los argumentos que manda el modelo. */
public final class ToolSchema {

    private ToolSchema() {}

    public static Map<String, Object> params(Map<String, Object> properties, String... required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of(required));
        return schema;
    }

    public static Map<String, Object> sinParametros() {
        return params(new LinkedHashMap<>());
    }

    public static Map<String, Object> texto(String descripcion) {
        return Map.of("type", "string", "description", descripcion);
    }

    public static Map<String, Object> listaDeTextos(String descripcion) {
        return Map.of("type", "array", "description", descripcion, "items", Map.of("type", "string"));
    }

    public static Map<String, Object> numero(String descripcion) {
        return Map.of("type", "number", "description", descripcion);
    }

    /** Campo que solo admite unos valores concretos. El modelo elige uno en vez de inventarlo. */
    public static Map<String, Object> unaDe(String descripcion, String... valores) {
        return Map.of("type", "string", "description", descripcion, "enum", List.of(valores));
    }

    /** Lee un argumento opcional. Devuelve null si no vino o vino vacio. */
    public static String opcional(Map<String, Object> args, String clave) {
        Object valor = args.get(clave);
        if (valor == null) {
            return null;
        }
        String texto = String.valueOf(valor).trim();
        return texto.isEmpty() || "null".equals(texto) ? null : texto;
    }

    /** Lee un argumento obligatorio. Falla con un mensaje que el modelo puede entender y corregir. */
    public static String obligatorio(Map<String, Object> args, String clave) {
        String valor = opcional(args, clave);
        if (valor == null) {
            throw new IllegalArgumentException("Falta el parametro obligatorio '" + clave + "'.");
        }
        return valor;
    }

    /** Lee un argumento numerico opcional. Devuelve null si no vino. */
    public static Double numeroOpcional(Map<String, Object> args, String clave) {
        String valor = opcional(args, clave);
        if (valor == null) {
            return null;
        }
        try {
            // Algunos modelos mandan el importe como "45", "45.0" o incluso "$45".
            return Double.valueOf(valor.replace("$", "").replace(",", "").trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "El valor de '" + clave + "' debe ser un numero, y llego '" + valor + "'.");
        }
    }

    /** Deja el valor actual cuando el modelo no manda uno nuevo: asi se editan campos sueltos. */
    public static <T> T oMantener(T nuevo, T actual) {
        return nuevo == null ? actual : nuevo;
    }

    @SuppressWarnings("unchecked")
    public static List<String> listaObligatoria(Map<String, Object> args, String clave) {
        Object valor = args.get(clave);
        if (valor instanceof List<?> lista && !lista.isEmpty()) {
            return lista.stream().map(String::valueOf).toList();
        }
        // Algunos modelos mandan un solo elemento como texto plano en lugar de lista.
        String texto = opcional(args, clave);
        if (texto != null) {
            return List.of(texto);
        }
        throw new IllegalArgumentException("Falta el parametro obligatorio '" + clave + "'.");
    }
}
