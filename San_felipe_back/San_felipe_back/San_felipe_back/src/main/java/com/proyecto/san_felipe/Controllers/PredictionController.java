package com.proyecto.san_felipe.Controllers;

import com.proyecto.san_felipe.Repository.DatosWekaRepository;
import com.proyecto.san_felipe.entities.DatosWeka;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class PredictionController {

    private static final Logger LOGGER = Logger.getLogger(PredictionController.class.getName());
    private Classifier classifier;
    private Instances dataStructure;
    private final DatosWekaRepository datosWekaRepository;

    public PredictionController(DatosWekaRepository datosWekaRepository) {
        this.datosWekaRepository = datosWekaRepository;
        try {
            ClassPathResource modelResource = new ClassPathResource("auto-model.model");
            classifier = (Classifier) weka.core.SerializationHelper.read(modelResource.getInputStream());
            LOGGER.info("Modelo lavadero_autos cargado exitosamente.");

            ClassPathResource arffResource = new ClassPathResource("lavadero_autos.arff");
            DataSource source = new DataSource(arffResource.getInputStream());
            dataStructure = source.getDataSet();
            dataStructure.setClassIndex(dataStructure.numAttributes() - 1); // Último atributo como clase
            LOGGER.info("Estructura lavadero_autos.arff cargada exitosamente.");
        } catch (Exception e) {
            LOGGER.severe("Error al inicializar PredictionController: " + e.getMessage());
            throw new RuntimeException("No se pudo inicializar el controlador de predicción", e);
        }
    }

    @PostMapping("/predecir")
    public ResponseEntity<Map<String, String>> predecir(@RequestBody DatosWeka datos) {
        try {
            LOGGER.info("Datos recibidos: idCliente=" + datos.getIdCliente() +
                    ", diaSemana=" + datos.getDiaSemana() +
                    ", hora=" + datos.getHora() +
                    ", clima=" + datos.getClima() +
                    ", temperatura=" + datos.getTemperatura() +
                    ", tipoServicio=" + datos.getTipoServicio() +
                    ", historialVisitas=" + datos.getHistorialVisitas() +
                    ", promocionesActivas=" + datos.getPromocionesActivas());
            if (dataStructure == null || classifier == null) {
                throw new IllegalStateException("Modelo o estructura no inicializados");
            }

            // Validate nominal attributes
            List<String> validDiaSemana = Arrays.asList("Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo");
            List<String> validClima = Arrays.asList("Soleado", "Lluvioso", "Nublado");
            List<String> validTipoServicio = Arrays.asList("Basico", "Completo", "Premium");
            List<String> validPromocionesActivas = Arrays.asList("Si", "No");

            if (!validDiaSemana.contains(datos.getDiaSemana())) {
                throw new IllegalArgumentException("Invalid Dia_Semana: " + datos.getDiaSemana());
            }
            if (!validClima.contains(datos.getClima())) {
                throw new IllegalArgumentException("Invalid Clima: " + datos.getClima());
            }
            if (!validTipoServicio.contains(datos.getTipoServicio())) {
                throw new IllegalArgumentException("Invalid Tipo_Servicio: " + datos.getTipoServicio());
            }
            if (!validPromocionesActivas.contains(datos.getPromocionesActivas())) {
                throw new IllegalArgumentException("Invalid Promociones_Activas: " + datos.getPromocionesActivas());
            }

            // Validate numeric attributes
            if (datos.getIdCliente() == null || datos.getIdCliente() < 0) {
                throw new IllegalArgumentException("ID_Cliente must be a non-negative number");
            }
            if (datos.getHora() == null || datos.getHora() < 0 || datos.getHora() > 23) {
                throw new IllegalArgumentException("Hora must be between 0 and 23");
            }
            if (datos.getTemperatura() == null || datos.getTemperatura() < -50 || datos.getTemperatura() > 50) {
                throw new IllegalArgumentException("Temperatura must be between -50 and 50");
            }
            if (datos.getHistorialVisitas() == null || datos.getHistorialVisitas() < 0) {
                throw new IllegalArgumentException("Historial_Visitas must be a non-negative number");
            }

            // Crear instancia con los atributos definidos en el ARFF
            Instance instance = new DenseInstance(dataStructure.numAttributes());
            instance.setDataset(dataStructure);

            LOGGER.info("Estructura del dataset: " + dataStructure.toSummaryString());
            LOGGER.info("Seteando valores...");

            // Asignar valores según el orden del ARFF
            instance.setValue(dataStructure.attribute("ID_Cliente"), datos.getIdCliente());
            instance.setValue(dataStructure.attribute("Dia_Semana"), datos.getDiaSemana());
            instance.setValue(dataStructure.attribute("Hora"), datos.getHora());
            instance.setValue(dataStructure.attribute("Clima"), datos.getClima());
            instance.setValue(dataStructure.attribute("Temperatura"), datos.getTemperatura());
            instance.setValue(dataStructure.attribute("Tipo_Servicio"), datos.getTipoServicio());
            instance.setValue(dataStructure.attribute("Historial_Visitas"), datos.getHistorialVisitas());
            instance.setValue(dataStructure.attribute("Promociones_Activas"), datos.getPromocionesActivas());

            LOGGER.info("Instancia creada: " + instance.toString());

            // Realizar la predicción
            double prediction = classifier.classifyInstance(instance);
            String resultado = dataStructure.classAttribute().value((int) prediction);
            LOGGER.info("Predicción obtenida: " + resultado);

            // Obtener la confianza
            double[] probabilities = classifier.distributionForInstance(instance);
            double confidence = probabilities[(int) prediction];
            DecimalFormat df = new DecimalFormat("#.#");
            String confidencePercentage = df.format(confidence * 100) + "%";

            // Guardar los datos en la base de datos
            datos.setPrediccion(resultado); // Asignar la predicción
            datos.setConfianza(confidencePercentage); // Asignar la confianza
            DatosWeka savedDatos = datosWekaRepository.save(datos);
            LOGGER.info("Datos guardados en la base de datos con id: " + savedDatos.getId());

            // Respuesta al frontend
            Map<String, String> response = new HashMap<>();
            response.put("prediccion", resultado);
            response.put("confianza", confidencePercentage);
            response.put("id", savedDatos.getId() != null ? savedDatos.getId() : "No generado");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOGGER.severe("Error: " + e.getMessage() + ", StackTrace: " + Arrays.toString(e.getStackTrace()));
            return ResponseEntity.status(500).body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    @GetMapping("/historial")
    public ResponseEntity<List<DatosWeka>> getHistorialPredicciones() {
        try {
            List<DatosWeka> historial = datosWekaRepository.findAll();
            if (historial.isEmpty()) {
                LOGGER.info("No hay predicciones en el historial.");
                return ResponseEntity.ok().body(historial);
            }
            LOGGER.info("Historial de predicciones obtenido: " + historial.size() + " registros.");
            return ResponseEntity.ok(historial);
        } catch (Exception e) {
            LOGGER.severe("Error al obtener el historial: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }
}