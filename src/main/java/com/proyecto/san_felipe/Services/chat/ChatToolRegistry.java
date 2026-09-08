package com.proyecto.san_felipe.Services.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reune todas las herramientas declaradas, las expone al modelo y despacha su ejecucion. */
@Component
public class ChatToolRegistry {

    private final Map<String, ChatTool> herramientas = new LinkedHashMap<>();

    public ChatToolRegistry(List<ChatTool> disponibles) {
        for (ChatTool herramienta : disponibles) {
            herramientas.put(herramienta.name(), herramienta);
        }
    }

    /** Definiciones en el formato de function calling de Groq (compatible con el de OpenAI). */
    public List<Map<String, Object>> definiciones() {
        return herramientas.values().stream()
                .map(herramienta -> Map.<String, Object>of(
                        "type", "function",
                        "function", Map.of(
                                "name", herramienta.name(),
                                "description", herramienta.description(),
                                "parameters", herramienta.parameters())))
                .toList();
    }

    public Set<String> nombres() {
        return herramientas.keySet();
    }

    /** Tokens aproximados que ocupan las definiciones que se envian en cada ronda. */
    public long tokensDeLasDefiniciones() {
        try {
            return CuotaLlm.estimarTokens(new ObjectMapper().writeValueAsString(definiciones()).length());
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Lo que pesa cada herramienta por separado. Las definiciones viajan enteras en cada
     * ronda, asi que este desglose es lo que dice donde recortar si la cuota aprieta.
     */
    public Map<String, Long> tokensPorHerramienta() {
        ObjectMapper mapeador = new ObjectMapper();
        Map<String, Long> pesos = new LinkedHashMap<>();
        for (ChatTool herramienta : herramientas.values()) {
            try {
                Map<String, Object> definicion = Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", herramienta.name(),
                                "description", herramienta.description(),
                                "parameters", herramienta.parameters()));
                pesos.put(herramienta.name(),
                        CuotaLlm.estimarTokens(mapeador.writeValueAsString(definicion).length()));
            } catch (Exception e) {
                pesos.put(herramienta.name(), -1L);
            }
        }
        return pesos;
    }

    /**
     * Ejecuta una herramienta. Los fallos se devuelven como dato, no como excepcion:
     * asi el modelo lee el error y puede corregir los argumentos o repreguntar al usuario.
     */
    public Object ejecutar(String nombre, Map<String, Object> argumentos) {
        ChatTool herramienta = herramientas.get(nombre);
        if (herramienta == null) {
            return Map.of("error", "La herramienta '" + nombre + "' no existe.");
        }
        try {
            return herramienta.execute(argumentos == null ? Map.of() : argumentos);
        } catch (Exception e) {
            String mensaje = e.getMessage() == null ? e.toString() : e.getMessage();
            return Map.of("error", mensaje);
        }
    }
}
