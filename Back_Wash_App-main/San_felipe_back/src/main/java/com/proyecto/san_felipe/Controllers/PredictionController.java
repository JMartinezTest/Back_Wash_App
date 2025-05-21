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
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class PredictionController {

    private final Classifier classifier;
    private final Instances dataStructure;
    private final DatosWekaRepository datosWekaRepository;

    public PredictionController(DatosWekaRepository datosWekaRepository) {
        this.datosWekaRepository = datosWekaRepository;

        try {
            ClassPathResource modelResource = new ClassPathResource("demanda_lavadero_clasificada.model");
            classifier = (Classifier) weka.core.SerializationHelper.read(modelResource.getInputStream());

            ClassPathResource arffResource = new ClassPathResource("demanda_lavadero_clasificada.arff");
            DataSource source = new DataSource(arffResource.getInputStream());
            dataStructure = source.getDataSet();
            dataStructure.setClassIndex(dataStructure.numAttributes() - 1); // Último atributo es la clase
        } catch (Exception e) {
            throw new RuntimeException("Error cargando modelo o estructura ARFF", e);
        }
    }

    @PostMapping("/predecir")
    public ResponseEntity<Map<String, String>> predecir(@RequestBody DatosWeka datos) {
        try {
            validarDatosEntrada(datos);

            // Crear una instancia según la estructura ARFF
            Instance instance = new DenseInstance(dataStructure.numAttributes());
            instance.setDataset(dataStructure);

            instance.setValue(dataStructure.attribute("Dia_Semana"), datos.getDiaSemana());
            instance.setValue(dataStructure.attribute("Jornada"), datos.getJornada());
            instance.setValue(dataStructure.attribute("Clima"), datos.getClima());
            instance.setValue(dataStructure.attribute("Temperatura"), datos.getTemperatura());
            instance.setValue(dataStructure.attribute("Tipo_Servicio"), datos.getTipoServicio());
            instance.setValue(dataStructure.attribute("Historial_Visitas"), datos.getHistorialVisitas());
            instance.setValue(dataStructure.attribute("Promociones_Activas"), datos.getPromocionesActivas());

            // Clasificar y obtener predicción
            double predictionIndex = classifier.classifyInstance(instance);
            String prediccion = dataStructure.classAttribute().value((int) predictionIndex);
            double[] distribution = classifier.distributionForInstance(instance);
            String confianza = new DecimalFormat("#.#").format(distribution[(int) predictionIndex] * 100) + "%";

            // Guardar la predicción en el campo `clientesEstimados`
            datos.setClientesEstimados(prediccion);

            // Guardar en la base de datos
            DatosWeka saved = datosWekaRepository.save(datos);

            Map<String, String> response = new HashMap<>();
            response.put("id", saved.getId());
            response.put("clientesEstimados", prediccion);
            response.put("confianza", confianza);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    @GetMapping("/historial")
    public ResponseEntity<List<DatosWeka>> getHistorial() {
        try {
            List<DatosWeka> historial = datosWekaRepository.findAll();
            return ResponseEntity.ok(historial);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    private void validarDatosEntrada(DatosWeka datos) {
        List<String> validDiaSemana = Arrays.asList("Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo");
        List<String> validJornada = Arrays.asList("Mañana", "Tarde", "Noche");
        List<String> validClima = Arrays.asList("Soleado", "Lluvioso", "Nublado");
        List<String> validTipoServicio = Arrays.asList("Basico", "Completo", "Premium");
        List<String> validPromociones = Arrays.asList("Si", "No");

        if (!validDiaSemana.contains(datos.getDiaSemana()))
            throw new IllegalArgumentException("Día inválido: " + datos.getDiaSemana());
        if (!validJornada.contains(datos.getJornada()))
            throw new IllegalArgumentException("Jornada inválida: " + datos.getJornada());
        if (!validClima.contains(datos.getClima()))
            throw new IllegalArgumentException("Clima inválido: " + datos.getClima());
        if (!validTipoServicio.contains(datos.getTipoServicio()))
            throw new IllegalArgumentException("Tipo de servicio inválido: " + datos.getTipoServicio());
        if (!validPromociones.contains(datos.getPromocionesActivas()))
            throw new IllegalArgumentException("Promoción inválida: " + datos.getPromocionesActivas());

        if (datos.getTemperatura() == null || datos.getTemperatura() < -50 || datos.getTemperatura() > 50)
            throw new IllegalArgumentException("Temperatura fuera de rango");
        if (datos.getHistorialVisitas() == null || datos.getHistorialVisitas() < 0)
            throw new IllegalArgumentException("Historial de visitas inválido");
    }
}
