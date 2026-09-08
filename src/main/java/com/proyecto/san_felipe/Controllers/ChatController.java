package com.proyecto.san_felipe.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.san_felipe.Services.chat.ChatToolRegistry;
import com.proyecto.san_felipe.Services.chat.LlmClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    /**
     * Cuantas rondas de herramientas se permiten antes de exigir una respuesta en texto.
     * Evita bucles indefinidos y acota el gasto: cada ronda es una llamada a Groq y su
     * cuota se mide en tokens por minuto.
     */
    private static final int MAX_RONDAS_DE_HERRAMIENTAS = 4;

    /**
     * Se envia la conversacion completa en cada ronda para que el asistente no pierda
     * el contexto, asi que este tope es lo que mas pesa despues de las herramientas:
     * son unos 2000 tokens que viajan en cada una de las rondas de cada consulta.
     * Se conservan los mensajes mas recientes. Ajustable con chat.historial.max-caracteres.
     */
    private static final int MAX_CARACTERES_DE_HISTORIAL_POR_DEFECTO = 8000;

    private static final String SYSTEM_PROMPT =
            "Eres el asistente operativo del Lavadero de Autos San Felipe. Ayudas al personal a "
            + "consultar informacion y a registrar operaciones en el sistema.\n"
            + "\n"
            + "Reglas:\n"
            + "- Tienes herramientas conectadas a la base de datos real. Usalas siempre para "
            + "responder sobre clientes, vehiculos, empleados, servicios, precios, lavados y "
            + "comisiones. Nunca inventes ni estimes esos datos.\n"
            + "- Antes de registrar algo, asegurate de tener todos los datos necesarios. Si falta "
            + "alguno, preguntaselo al usuario en vez de suponerlo.\n"
            + "- Tambien puedes corregir y eliminar registros. Al corregir, manda solo los campos "
            + "que cambian: el resto se conserva. Antes de eliminar algo, di en una frase que vas "
            + "a borrar y espera a que el usuario lo confirme; no se puede deshacer.\n"
            + "- Para corregir o eliminar un lavado necesitas su referencia: buscalo primero con "
            + "consultar_lavados y usa la referencia que devuelve.\n"
            + "- Si una herramienta devuelve un error, explicaselo al usuario en lenguaje claro y "
            + "pidele el dato que falta o esta ambiguo.\n"
            + "- La comision de un empleado es el 35% de los servicios que realizo, pero calculala "
            + "siempre con la herramienta correspondiente.\n"
            + "- No repitas una herramienta con los mismos argumentos: si ya no encontro nada, "
            + "diselo al usuario en vez de volver a intentarlo.\n"
            + "- No puedes prever la demanda futura ni estimarla a partir del historial. Si te la "
            + "piden, responde directamente que esa estimacion esta en la pantalla de "
            + "predicciones, sin usar ninguna herramienta.\n"
            + "- Los importes van en pesos y se escriben con el simbolo $ (por ejemplo $50.75). "
            + "Nunca uses otra moneda ni inventes su simbolo.\n"
            + "- Responde en espaniol, de forma breve, amable y profesional.\n"
            + "- Al confirmar un registro, repite los datos que quedaron guardados.";

    @Value("${chat.historial.max-caracteres:" + MAX_CARACTERES_DE_HISTORIAL_POR_DEFECTO + "}")
    private int maxCaracteresDeHistorial;

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private ChatToolRegistry herramientas;

    private final ObjectMapper mapeadorJson = new ObjectMapper();

    /** Utilidad de diagnostico: que herramientas tiene disponibles el asistente. */
    @GetMapping("/tools")
    public Map<String, Object> listarHerramientas() {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("modelo", llmClient.modelo());
        respuesta.put("configurado", llmClient.estaConfigurado());
        respuesta.put("herramientas", herramientas.nombres());
        // Las definiciones viajan en cada ronda, asi que su peso marca cuantas caben
        // en la cuota por minuto de Groq.
        respuesta.put("tokens_de_las_definiciones", herramientas.tokensDeLasDefiniciones());
        respuesta.put("tokens_por_herramienta", herramientas.tokensPorHerramienta());
        return respuesta;
    }

    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@RequestBody ChatRequest request) {
        if (!llmClient.estaConfigurado()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "error", "El asistente no esta configurado: falta la clave del proveedor. Define "
                            + "la variable de entorno LLM_API_KEY o ponla en application-local.properties."));
        }
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El mensaje esta vacio."));
        }

        try {
            List<Map<String, Object>> mensajes = new ArrayList<>();
            mensajes.add(mensaje("system", SYSTEM_PROMPT + "\n\nLa fecha de hoy es " + LocalDate.now() + "."));
            mensajes.addAll(historialReciente(request.getHistory()));
            mensajes.add(mensaje("user", request.getMessage()));

            // Registro de lo que el asistente ejecuto, para mostrarlo en la interfaz.
            List<String> acciones = new ArrayList<>();

            for (int ronda = 0; ronda < MAX_RONDAS_DE_HERRAMIENTAS; ronda++) {
                Map<String, Object> respuesta = llmClient.completar(mensajes, herramientas.definiciones());
                List<Map<String, Object>> llamadas = extraerLlamadas(respuesta);

                if (llamadas.isEmpty()) {
                    return ResponseEntity.ok(
                            new ChatResponse(conRespaldo(texto(respuesta.get("content"))), acciones));
                }

                mensajes.add(respuesta);
                for (Map<String, Object> llamada : llamadas) {
                    mensajes.add(ejecutar(llamada, acciones));
                }
            }

            // Se agoto el presupuesto de herramientas: se pide una respuesta final en texto.
            return ResponseEntity.ok(new ChatResponse(respuestaDeCierre(mensajes), acciones));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error: " + e.getMessage()));
        }
    }

    /**
     * Pide la respuesta final sin herramientas. Se le indica explicitamente que ya no puede
     * usarlas, porque si lo intenta Groq rechaza la generacion con un 400; si aun asi ocurre,
     * se devuelve un aviso en vez de propagar el error.
     */
    private String respuestaDeCierre(List<Map<String, Object>> mensajes) {
        List<Map<String, Object>> conInstruccion = new ArrayList<>(mensajes);
        conInstruccion.add(mensaje("system",
                "Ya no puedes usar herramientas. Responde al usuario en texto con la informacion "
                        + "que obtuviste. Si te falta algun dato, pideselo."));
        try {
            Map<String, Object> cierre = llmClient.completar(conInstruccion, List.of());
            String contenido = texto(cierre.get("content"));
            if (!contenido.isBlank()) {
                return contenido;
            }
        } catch (Exception e) {
            // Cae al mensaje de reserva de abajo.
        }
        return "Consulte los datos pero no consegui redactar la respuesta. "
                + "Puedes reformular la pregunta o pedirmelo por partes?";
    }

    /**
     * El modelo a veces termina el turno sin texto y sin pedir herramientas. Sin esto la
     * interfaz pintaria una burbuja vacia, que parece que el asistente esta averiado.
     */
    private String conRespaldo(String contenido) {
        return contenido.isBlank()
                ? "No consegui redactar la respuesta. Puedes repetirme lo que necesitas?"
                : contenido;
    }

    /** Ejecuta una tool_call y arma el mensaje de rol "tool" que espera el modelo. */
    private Map<String, Object> ejecutar(Map<String, Object> llamada, List<String> acciones) {
        @SuppressWarnings("unchecked")
        Map<String, Object> funcion = (Map<String, Object>) llamada.get("function");
        String nombre = texto(funcion.get("name"));

        Map<String, Object> argumentos;
        try {
            String crudos = texto(funcion.get("arguments"));
            argumentos = crudos.isBlank() ? Map.of() : mapeadorJson.readValue(crudos, Map.class);
        } catch (Exception e) {
            argumentos = Map.of();
        }

        Object resultado = herramientas.ejecutar(nombre, argumentos);
        acciones.add(nombre);

        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("role", "tool");
        mensaje.put("tool_call_id", texto(llamada.get("id")));
        mensaje.put("name", nombre);
        mensaje.put("content", serializar(resultado));
        return mensaje;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extraerLlamadas(Map<String, Object> respuesta) {
        Object llamadas = respuesta.get("tool_calls");
        if (llamadas instanceof List<?> lista && !lista.isEmpty()) {
            return (List<Map<String, Object>>) lista;
        }
        return List.of();
    }

    /**
     * Convierte al formato de la API toda la conversacion que quepa en el presupuesto,
     * recorriendola del final al principio para conservar siempre lo mas reciente.
     */
    private List<Map<String, Object>> historialReciente(List<ChatMessage> historial) {
        if (historial == null || historial.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> convertidos = new ArrayList<>();
        int caracteres = 0;
        for (int i = historial.size() - 1; i >= 0; i--) {
            ChatMessage entrada = historial.get(i);
            if (entrada == null || entrada.getText() == null || entrada.getText().isBlank()) {
                continue;
            }
            caracteres += entrada.getText().length();
            if (caracteres > maxCaracteresDeHistorial) {
                break;
            }
            String rol = "user".equals(entrada.getRole()) ? "user" : "assistant";
            convertidos.add(0, mensaje(rol, entrada.getText()));
        }
        return convertidos;
    }

    private Map<String, Object> mensaje(String rol, String contenido) {
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("role", rol);
        mensaje.put("content", contenido);
        return mensaje;
    }

    private String serializar(Object valor) {
        try {
            return mapeadorJson.writeValueAsString(valor);
        } catch (Exception e) {
            return String.valueOf(valor);
        }
    }

    private String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor);
    }

    static class ChatRequest {
        private String message;
        private List<ChatMessage> history;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public List<ChatMessage> getHistory() { return history; }
        public void setHistory(List<ChatMessage> history) { this.history = history; }
    }

    static class ChatMessage {
        private String role;
        private String text;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    static class ChatResponse {
        private final String response;
        private final List<String> actions;

        public ChatResponse(String response, List<String> actions) {
            this.response = response;
            this.actions = actions;
        }

        public String getResponse() { return response; }
        public List<String> getActions() { return actions; }
    }
}
