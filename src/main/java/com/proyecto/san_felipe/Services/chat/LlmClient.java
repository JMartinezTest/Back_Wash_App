package com.proyecto.san_felipe.Services.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente de chat completions con formato OpenAI, respetando la cuota del proveedor.
 *
 * Sirve para Cerebras (el proveedor por defecto), Groq o cualquier otro compatible:
 * solo cambian llm.api.url, llm.api.key y llm.model.
 */
@Component
public class LlmClient {

    /** Cuanto se acepta esperar a que se reponga la cuota antes de rendirse. */
    private static final long ESPERA_MAXIMA_MS = 15_000;

    private static final int MAX_TOKENS_DE_RESPUESTA = 1024;

    /** Reintentos cuando el fallo es de la generacion del modelo, no de la peticion. */
    private static final int MAX_REINTENTOS_POR_GENERACION = 2;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapeadorJson = new ObjectMapper();

    @Autowired
    private CuotaLlm cuota;

    /** Configurable para poder apuntar a un proxy o a un servidor de pruebas. */
    @Value("${llm.api.url:https://api.cerebras.ai/v1/chat/completions}")
    private String apiUrl;

    @Value("${llm.api.key:}")
    private String apiKey;

    @Value("${llm.model:qwen-3.8-27b}")
    private String model;

    public boolean estaConfigurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String modelo() {
        return model;
    }

    /**
     * Pide una respuesta al modelo. Si {@code herramientas} no viene vacia, el modelo puede
     * responder con tool_calls en lugar de texto.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> completar(List<Map<String, Object>> mensajes, List<Map<String, Object>> herramientas) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.APPLICATION_JSON);
        cabeceras.setBearerAuth(apiKey);

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("model", model);
        cuerpo.put("messages", mensajes);
        cuerpo.put("temperature", 0.1);
        cuerpo.put("max_tokens", MAX_TOKENS_DE_RESPUESTA);
        if (herramientas != null && !herramientas.isEmpty()) {
            cuerpo.put("tools", herramientas);
            cuerpo.put("tool_choice", "auto");
            // Los modelos gpt-oss cuelan su canal interno en el nombre de la herramienta
            // ("buscar_vehiculos<|channel|>commentary") cuando razonan de mas. Con el
            // razonamiento bajo eso desaparece y ademas se gastan menos tokens.
            if (model != null && model.contains("gpt-oss")) {
                cuerpo.put("reasoning_effort", "low");
            }
        }

        // Antes de gastar una ronda se comprueba que quepa en la cuota que queda.
        long coste = estimarCoste(cuerpo);
        cuota.esperarSiNoCabe(coste, ESPERA_MAXIMA_MS);
        cuota.descontar(coste);

        ResponseEntity<Map> respuesta = enviar(new HttpEntity<>(cuerpo, cabeceras));

        Map<String, Object> datos = respuesta.getBody();
        if (datos == null) {
            throw new IllegalStateException("Groq no devolvio contenido.");
        }
        List<Map<String, Object>> opciones = (List<Map<String, Object>>) datos.get("choices");
        if (opciones == null || opciones.isEmpty()) {
            throw new IllegalStateException("Groq no devolvio ninguna respuesta.");
        }
        return (Map<String, Object>) opciones.get(0).get("message");
    }

    /**
     * Envia la peticion tolerando los dos fallos transitorios habituales:
     *
     * - 429 si la cuota se agoto entre medias: se espera lo que indique y se reintenta.
     * - 400 tool_use_failed: algunos modelos generan mal el nombre de la herramienta
     *   (por ejemplo "buscar_vehiculos<|channel|>commentary") y el proveedor rechaza la
     *   generacion. Es aleatorio, asi que se vuelve a pedir.
     */
    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> enviar(HttpEntity<Map<String, Object>> peticion) {
        IllegalStateException ultimoFallo = null;
        for (int intento = 0; intento <= MAX_REINTENTOS_POR_GENERACION; intento++) {
            try {
                ResponseEntity<Map> respuesta = restTemplate.exchange(
                        apiUrl, HttpMethod.POST, peticion, Map.class);
                cuota.registrar(respuesta.getHeaders());
                return respuesta;
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (!cuota.esperarTrasRechazo(e.getResponseBodyAsString(), ESPERA_MAXIMA_MS)) {
                    throw new IllegalStateException(
                            "El asistente alcanzo el limite de uso del proveedor. Intentalo de nuevo en un momento.");
                }
                ultimoFallo = new IllegalStateException(
                        "El asistente alcanzo el limite de uso del proveedor. Intentalo de nuevo en un momento.");
            } catch (HttpClientErrorException.BadRequest e) {
                if (!e.getResponseBodyAsString().contains("tool_use_failed")) {
                    throw e;
                }
                ultimoFallo = new IllegalStateException(
                        "El modelo genero una llamada a herramienta invalida. Intentalo de nuevo.");
            }
        }
        throw ultimoFallo;
    }

    /** Tokens que se calcula que va a costar la peticion, entrada mas respuesta. */
    private long estimarCoste(Map<String, Object> cuerpo) {
        try {
            return CuotaLlm.estimarTokens(mapeadorJson.writeValueAsString(cuerpo).length())
                    + MAX_TOKENS_DE_RESPUESTA;
        } catch (Exception e) {
            return MAX_TOKENS_DE_RESPUESTA;
        }
    }
}
