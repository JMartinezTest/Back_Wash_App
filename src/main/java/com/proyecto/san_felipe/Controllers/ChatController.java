package com.proyecto.san_felipe.Controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    private static final String SYSTEM_PROMPT =
            "Eres el asistente virtual inteligente del Lavadero de Autos San Felipe. " +
            "Tu rol es ayudar al personal del negocio con información sobre: " +
            "- Registro y consulta de lavados realizados. " +
            "- Servicios ofrecidos: Básico, Completo y Premium (precios, duración, diferencias). " +
            "- Gestión de clientes: historial, datos de contacto, vehículos registrados. " +
            "- Empleados: asignación de lavados, cálculo de comisiones (35% del total). " +
            "- Vehículos: consulta por placa, marca, color. " +
            "- Predicción de demanda: interpretar resultados del modelo de machine learning. " +
            "- Consejos sobre cómo mejorar la atención al cliente y aumentar las ventas. " +
            "Responde siempre en español, de forma amable, clara y profesional. " +
            "Si no sabes algo específico del negocio, da una respuesta útil y general sobre lavaderos de autos.";

    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@RequestBody ChatRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> systemInstruction = new HashMap<>();
            systemInstruction.put("parts", List.of(Map.of("text", SYSTEM_PROMPT)));

            Map<String, Object> userContent = new HashMap<>();
            userContent.put("role", "user");
            userContent.put("parts", List.of(Map.of("text", request.getMessage())));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("system_instruction", systemInstruction);
            requestBody.put("contents", List.of(userContent));

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 1024);
            requestBody.put("generationConfig", generationConfig);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    GEMINI_API_URL + geminiApiKey,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getBody() != null) {
                List<Map<String, Object>> candidates =
                        (List<Map<String, Object>>) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    String text = (String) parts.get(0).get("text");
                    return ResponseEntity.ok(new ChatResponse(text));
                }
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se recibió respuesta del modelo"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar la solicitud: " + e.getMessage()));
        }
    }

    static class ChatRequest {
        private String message;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    static class ChatResponse {
        private String response;
        public ChatResponse(String response) { this.response = response; }
        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
    }
}
