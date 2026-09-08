package com.proyecto.san_felipe.Services.chat;

import com.proyecto.san_felipe.Repository.DatosWekaRepository;
import com.proyecto.san_felipe.Services.clima.ClimaService;
import com.proyecto.san_felipe.entities.DatosWeka;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.proyecto.san_felipe.Services.chat.ToolSchema.opcional;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.params;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.sinParametros;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.texto;

/**
 * Herramientas de clima e historial de predicciones.
 *
 * La prevision de demanda en vivo no se expone al asistente: su definicion costaba 219
 * tokens en cada ronda de cada consulta, y la funcion ya tiene su propia pantalla. Para
 * devolversela, basta con reponer el @Bean que la declaraba (esta en el historial de git)
 * y la regla del prompt que la anunciaba.
 */
@Configuration
public class PredictionTools {

    /** Las fechas se envian escritas: un timestamp numerico no le dice nada al modelo. */
    private static final java.text.SimpleDateFormat FECHA =
            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Bean
    public ChatTool climaActual(ClimaService climaService) {
        return new ChatTool(
                "clima_actual",
                "Clima y temperatura ahora mismo en el lavadero.",
                sinParametros(),
                args -> {
                    ClimaService.Clima clima = climaService.actual();
                    if (clima == null) {
                        return Map.of("error", "No se pudo consultar el clima en este momento.");
                    }
                    return clima.comoMapa();
                });
    }

    @Bean
    public ChatTool consultarPredicciones(DatosWekaRepository repositorio) {
        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("limite", texto("Cuantas devolver, de la mas reciente hacia atras. Por defecto 10."));
        return new ChatTool(
                "consultar_predicciones",
                "Predicciones guardadas en el historial, con las condiciones de cada una.",
                params(propiedades),
                args -> {
                    String limite = opcional(args, "limite");
                    int cuantas = limite == null ? 10 : (int) numero(limite, "limite");

                    List<DatosWeka> guardadas = new ArrayList<>(repositorio.findAll());
                    guardadas.sort(Comparator.comparing(DatosWeka::getCreadoEn,
                            Comparator.nullsLast(Comparator.reverseOrder())));

                    List<Map<String, Object>> vista = new ArrayList<>();
                    for (DatosWeka d : guardadas.stream().limit(Math.max(1, cuantas)).toList()) {
                        Map<String, Object> fila = new LinkedHashMap<>();
                        fila.put("consultada_el", d.getCreadoEn() == null ? "(sin fecha)"
                                : FECHA.format(d.getCreadoEn()));
                        fila.put("dia", d.getDiaSemana());
                        fila.put("hora", d.getHora() == null ? null : d.getHora().intValue());
                        fila.put("clima", d.getClima());
                        fila.put("temperatura", d.getTemperatura());
                        fila.put("promociones", d.getPromocionesActivas());
                        fila.put("demanda_alta", "Si".equals(d.getPrediccion()));
                        fila.put("confianza", d.getConfianza());
                        vista.add(fila);
                    }
                    Map<String, Object> respuesta = new LinkedHashMap<>();
                    respuesta.put("total_guardadas", guardadas.size());
                    respuesta.put("predicciones", vista);
                    return respuesta;
                });
    }

    private static double numero(String valor, String campo) {
        try {
            return Double.parseDouble(valor.replace(",", ".").replaceAll("[^0-9.\\-]", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El valor de '" + campo + "' no es un numero: " + valor);
        }
    }
}
