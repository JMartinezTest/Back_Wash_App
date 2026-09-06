package com.proyecto.san_felipe.Services.clima;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Consulta el clima actual del lavadero en Open-Meteo (gratuita y sin clave).
 *
 * El clima y la temperatura son dos de las variables del modelo de demanda. Si no
 * se registran en cada lavado, el historial nunca sirve para reentrenar el modelo
 * con datos propios; por eso se capturan automaticamente al registrar el servicio.
 */
@Service
public class ClimaService {

    private static final Logger LOGGER = Logger.getLogger(ClimaService.class.getName());

    /** La respuesta se reutiliza un rato: el tiempo no cambia entre dos lavados seguidos. */
    private static final Duration VIGENCIA = Duration.ofMinutes(15);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${clima.api.url:https://api.open-meteo.com/v1/forecast}")
    private String apiUrl;

    @Value("${clima.latitud:10.3910}")
    private double latitud;

    @Value("${clima.longitud:-75.4794}")
    private double longitud;

    @Value("${clima.zona-horaria:America/Bogota}")
    private String zonaHoraria;

    @Value("${clima.habilitado:true}")
    private boolean habilitado;

    private volatile Clima enCache;
    private volatile Instant consultadoEn;

    /** Clima actual, o null si no se pudo obtener. Nunca lanza excepcion. */
    public Clima actual() {
        if (!habilitado) {
            return null;
        }
        Clima cacheado = enCache;
        if (cacheado != null && consultadoEn != null
                && Duration.between(consultadoEn, Instant.now()).compareTo(VIGENCIA) < 0) {
            return cacheado;
        }
        try {
            String url = String.format(
                    "%s?latitude=%s&longitude=%s&current=temperature_2m,weather_code&timezone=%s",
                    apiUrl, latitud, longitud, zonaHoraria);

            @SuppressWarnings("unchecked")
            Map<String, Object> respuesta = restTemplate.getForObject(url, Map.class);
            if (respuesta == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> actual = (Map<String, Object>) respuesta.get("current");
            if (actual == null) {
                return null;
            }

            double temperatura = ((Number) actual.get("temperature_2m")).doubleValue();
            int codigo = ((Number) actual.get("weather_code")).intValue();

            Clima clima = new Clima(descripcionSegunCodigo(codigo), temperatura, codigo);
            enCache = clima;
            consultadoEn = Instant.now();
            return clima;

        } catch (Exception e) {
            // Un fallo del servicio externo no puede impedir registrar un lavado.
            LOGGER.warning("No se pudo obtener el clima: " + e.getMessage());
            return null;
        }
    }

    /**
     * Traduce el codigo WMO de Open-Meteo a las tres categorias con las que se
     * entreno el modelo. https://open-meteo.com/en/docs
     */
    static String descripcionSegunCodigo(int codigo) {
        if (codigo <= 1) {
            return "Soleado";              // 0 despejado, 1 mayormente despejado
        }
        if (codigo <= 3 || codigo == 45 || codigo == 48) {
            return "Nublado";              // 2-3 nubes, 45/48 niebla
        }
        return "Lluvioso";                 // llovizna, lluvia, chubascos y tormenta
    }

    /** Instantanea del tiempo en el lavadero. */
    public static class Clima {
        private final String descripcion;
        private final double temperatura;
        private final int codigo;

        public Clima(String descripcion, double temperatura, int codigo) {
            this.descripcion = descripcion;
            this.temperatura = temperatura;
            this.codigo = codigo;
        }

        public String getDescripcion() { return descripcion; }
        public double getTemperatura() { return temperatura; }
        public int getCodigo() { return codigo; }

        public Map<String, Object> comoMapa() {
            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("clima", descripcion);
            mapa.put("temperatura", temperatura);
            mapa.put("codigo", codigo);
            return mapa;
        }
    }
}
