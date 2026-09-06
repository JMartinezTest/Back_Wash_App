package com.proyecto.san_felipe.Services.chat;

import com.proyecto.san_felipe.Repository.DatosWekaRepository;
import com.proyecto.san_felipe.Services.clima.ClimaService;
import com.proyecto.san_felipe.Services.prediccion.PrediccionService;
import com.proyecto.san_felipe.Services.prediccion.PrediccionService.Condiciones;
import com.proyecto.san_felipe.Services.prediccion.PrediccionService.Resultado;
import com.proyecto.san_felipe.entities.DatosWeka;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.proyecto.san_felipe.Services.chat.ToolSchema.obligatorio;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.opcional;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.params;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.sinParametros;
import static com.proyecto.san_felipe.Services.chat.ToolSchema.texto;

/** Herramientas de prevision de demanda y clima. */
@Configuration
public class PredictionTools {

    /** Sin datos del cliente se asume un cliente medio; el modelo apenas usa esta variable. */
    private static final int VISITAS_POR_DEFECTO = 5;

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
    public ChatTool previsionDeDemanda(PrediccionService prediccion, ClimaService climaService) {
        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("dia", texto("Dia: Lunes, Martes, Miercoles, Jueves, Viernes, Sabado o Domingo."));
        propiedades.put("hora", texto("Hora concreta (0-23). Omitela para ver todo el dia."));
        propiedades.put("clima", texto("Soleado, Lluvioso o Nublado. Si se omite se usa el clima actual."));
        propiedades.put("temperatura", texto("Grados centigrados. Si se omite se usa la temperatura actual."));
        propiedades.put("promociones", texto("Si o No. Por defecto No."));
        propiedades.put("visitas", texto("Visitas previas del cliente. Por defecto 5."));
        return new ChatTool(
                "prevision_de_demanda",
                "Predice si habra demanda alta. Sin 'hora' devuelve la prevision de todo el dia "
                        + "por franjas; con 'hora' solo esa. Usala para saber cuando reforzar personal.",
                params(propiedades, "dia"),
                args -> {
                    // Lo que no se indique se toma del tiempo real, para no inventarlo.
                    ClimaService.Clima ahora = climaService.actual();
                    String clima = opcional(args, "clima");
                    String temperatura = opcional(args, "temperatura");

                    if (clima == null && ahora != null) {
                        clima = ahora.getDescripcion();
                    }
                    double grados = temperatura != null ? numero(temperatura, "temperatura")
                            : (ahora != null ? ahora.getTemperatura() : 25);

                    String promociones = opcional(args, "promociones");
                    String visitas = opcional(args, "visitas");

                    Condiciones condiciones = new Condiciones(
                            capitalizar(obligatorio(args, "dia")),
                            clima == null ? "Soleado" : capitalizar(clima),
                            grados,
                            visitas == null ? VISITAS_POR_DEFECTO : (int) numero(visitas, "visitas"),
                            promociones == null ? "No" : capitalizar(promociones));

                    Map<String, Object> respuesta = new LinkedHashMap<>();
                    respuesta.put("dia", condiciones.diaSemana());
                    respuesta.put("clima", condiciones.clima());
                    respuesta.put("temperatura", condiciones.temperatura());
                    respuesta.put("promociones", condiciones.promocionesActivas());
                    respuesta.put("clima_tomado_del_servicio_meteorologico",
                            opcional(args, "clima") == null && ahora != null);

                    String hora = opcional(args, "hora");
                    if (hora != null) {
                        Resultado r = prediccion.predecir(condiciones, (int) numero(hora, "hora"));
                        respuesta.put("hora", r.hora());
                        respuesta.put("demanda_alta", r.demandaAlta());
                        respuesta.put("confianza", r.confianza() + "%");
                        return respuesta;
                    }

                    List<Resultado> franjas = prediccion.previsionDelDia(condiciones, null, null);
                    List<Integer> altas = franjas.stream()
                            .filter(Resultado::demandaAlta).map(Resultado::hora).toList();

                    List<Map<String, Object>> detalle = new ArrayList<>();
                    for (Resultado r : franjas) {
                        Map<String, Object> franja = new LinkedHashMap<>();
                        franja.put("hora", r.hora());
                        franja.put("demanda_alta", r.demandaAlta());
                        franja.put("confianza", r.confianza() + "%");
                        detalle.add(franja);
                    }
                    respuesta.put("horario_evaluado",
                            PrediccionService.HORA_APERTURA + ":00 a " + PrediccionService.HORA_CIERRE + ":00");
                    respuesta.put("horas_con_demanda_alta", altas);
                    respuesta.put("franjas", detalle);
                    return respuesta;
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

    /** El modelo espera "Sabado", "Soleado", "Si": mayuscula inicial y resto en minuscula. */
    private static String capitalizar(String valor) {
        String limpio = valor.trim();
        return limpio.isEmpty() ? limpio
                : Character.toUpperCase(limpio.charAt(0)) + limpio.substring(1).toLowerCase();
    }

    private static double numero(String valor, String campo) {
        try {
            return Double.parseDouble(valor.replace(",", ".").replaceAll("[^0-9.\\-]", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El valor de '" + campo + "' no es un numero: " + valor);
        }
    }
}
