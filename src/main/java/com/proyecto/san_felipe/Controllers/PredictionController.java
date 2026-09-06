package com.proyecto.san_felipe.Controllers;

import com.proyecto.san_felipe.Repository.DatosWekaRepository;
import com.proyecto.san_felipe.Services.prediccion.PrediccionService;
import com.proyecto.san_felipe.Services.prediccion.PrediccionService.Condiciones;
import com.proyecto.san_felipe.Services.prediccion.PrediccionService.Resultado;
import com.proyecto.san_felipe.entities.DatosWeka;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api")
public class PredictionController {

    private static final Logger LOGGER = Logger.getLogger(PredictionController.class.getName());

    @Autowired
    private PrediccionService prediccionService;

    private final DatosWekaRepository datosWekaRepository;

    public PredictionController(DatosWekaRepository datosWekaRepository) {
        this.datosWekaRepository = datosWekaRepository;
    }

    /** Consulta puntual: predice una hora concreta y la deja registrada en el historial. */
    @PostMapping("/predecir")
    public ResponseEntity<?> predecir(@RequestBody DatosWeka datos) {
        try {
            Condiciones condiciones = condicionesDe(datos);
            int hora = datos.getHora() == null ? PrediccionService.HORA_APERTURA : datos.getHora().intValue();
            Resultado resultado = prediccionService.predecir(condiciones, hora);

            datos.setPrediccion(resultado.demandaAlta() ? "Si" : "No");
            datos.setConfianza(resultado.confianza() + "%");
            datos.setCreadoEn(new Date());
            DatosWeka guardado = datosWekaRepository.save(datos);

            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("prediccion", datos.getPrediccion());
            respuesta.put("confianza", datos.getConfianza());
            respuesta.put("id", guardado.getId());
            return ResponseEntity.ok(respuesta);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            LOGGER.severe("Error al predecir: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    /**
     * Previsión de un día completo: el modelo se ejecuta hora a hora para ver de un
     * vistazo en qué franjas se espera demanda alta. No se guarda en el historial:
     * son consultas exploratorias, no registros.
     */
    @PostMapping("/prevision-dia")
    public ResponseEntity<?> previsionDelDia(@RequestBody DatosWeka datos) {
        try {
            List<Resultado> franjas = prediccionService.previsionDelDia(
                    condicionesDe(datos), datos.getHoraInicio(), datos.getHoraFin());

            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("franjas", franjas.stream().map(Resultado::comoMapa).toList());
            respuesta.put("horasConDemandaAlta", franjas.stream().filter(Resultado::demandaAlta).count());
            respuesta.put("horasEvaluadas", franjas.size());
            return ResponseEntity.ok(respuesta);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            LOGGER.severe("Error en la prevision del dia: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    @GetMapping("/historial")
    public ResponseEntity<List<DatosWeka>> getHistorialPredicciones() {
        try {
            // Lo más reciente primero; los registros antiguos no tienen fecha.
            List<DatosWeka> historial = new ArrayList<>(datosWekaRepository.findAll());
            historial.sort(Comparator.comparing(
                    DatosWeka::getCreadoEn, Comparator.nullsLast(Comparator.reverseOrder())));
            return ResponseEntity.ok(historial);
        } catch (Exception e) {
            LOGGER.severe("Error al obtener el historial: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    private Condiciones condicionesDe(DatosWeka datos) {
        return new Condiciones(datos.getDiaSemana(), datos.getClima(), datos.getTemperatura(),
                datos.getHistorialVisitas(), datos.getPromocionesActivas());
    }
}
