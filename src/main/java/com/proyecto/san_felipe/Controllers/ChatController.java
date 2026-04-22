package com.proyecto.san_felipe.Controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Value("${openrouter.api.key}")
    private String openRouterApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions";

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
            "Responde siempre en español, de forma amable, clara y profesional.";

    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@RequestBody ChatRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openRouterApiKey);
            headers.add("HTTP-Referer", "https://front-wash-app-production.up.railway.app");
            headers.add("X-Title", "San Felipe Assistant");

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
            messages.add(Map.of("role", "user", "content", request.getMessage()));

            Map<String, Object> body = new HashMap<>();
            body.put("model", "openai/gpt-4o-mini");
            body.put("messages", messages);
            body.put("temperature", 0.7);
            body.put("max_tokens", 1024);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    OPENROUTER_API_URL, HttpMethod.POST, entity, Map.class);

            if (response.getBody() != null) {
                List<Map<String, Object>> choices =
                        (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message =
                            (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    return ResponseEntity.ok(new ChatResponse(content));
                }
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se recibió respuesta del modelo"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error: " + e.getMessage()));
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
