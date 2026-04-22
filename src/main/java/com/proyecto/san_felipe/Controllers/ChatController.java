package com.proyecto.san_felipe.Controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    // Cambiamos a las variables de OpenRouter
    @Value("${openrouter.api.key}")
    private String openRouterApiKey;

    @Value("${openrouter.model}")
    private String openRouterModel;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions";

    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@RequestBody ChatRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openRouterApiKey);

            // ⚠️ Requeridos por OpenRouter para solicitudes directas desde servidores
            headers.add("HTTP-Referer", "https://tusitio.com"); // Cambia por tu dominio real
            headers.add("X-Title", "San Felipe Assistant");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", openRouterModel);
            requestBody.put("messages", createMessages(request.getMessage()));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 1024);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                OPENROUTER_API_URL,
                
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");

                    return ResponseEntity.ok(new ChatResponse(content));
                }
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se recibió respuesta del modelo"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar la solicitud: " + e.getMessage()));
        }
    }

    private List<Map<String, String>> createMessages(String userMessage) {
        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", """
            Eres un asistente virtual del sistema de lavado de autos San Felipe.
            Ayudas con información sobre lavados, servicios, empleados, clientes y vehículos.
            Responde de manera concisa, amable y profesional.
        """);
        messages.add(systemMessage);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        return messages;
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
