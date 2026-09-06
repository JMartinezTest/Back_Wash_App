package com.proyecto.san_felipe.Services.prediccion;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Modelo de demanda del lavadero (arbol J48 entrenado con Weka).
 *
 * La logica vive aqui y no en el controlador para que la usen por igual la
 * pantalla de predicciones y las herramientas del asistente.
 */
@Service
public class PrediccionService {

    private static final Logger LOGGER = Logger.getLogger(PrediccionService.class.getName());

    public static final List<String> DIAS = Arrays.asList(
            "Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo");
    public static final List<String> CLIMAS = Arrays.asList("Soleado", "Lluvioso", "Nublado");
    public static final List<String> SI_NO = Arrays.asList("Si", "No");

    /** Horario habitual del lavadero, usado cuando no se indica otro rango. */
    public static final int HORA_APERTURA = 8;
    public static final int HORA_CIERRE = 20;

    private final Classifier clasificador;
    private final Instances estructura;

    public PrediccionService() {
        try {
            ClassPathResource modelo = new ClassPathResource("auto-model.model");
            clasificador = (Classifier) weka.core.SerializationHelper.read(modelo.getInputStream());

            ClassPathResource arff = new ClassPathResource("lavadero_autos.arff");
            estructura = new DataSource(arff.getInputStream()).getDataSet();
            estructura.setClassIndex(estructura.numAttributes() - 1);
            LOGGER.info("Modelo de demanda cargado.");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar el modelo de demanda", e);
        }
    }

    /** Condiciones sobre las que se pregunta al modelo. */
    public record Condiciones(String diaSemana, String clima, Double temperatura,
                              Integer historialVisitas, String promocionesActivas) {

        public void validar() {
            exigir(DIAS, diaSemana, "dia de la semana");
            exigir(CLIMAS, clima, "clima");
            exigir(SI_NO, promocionesActivas, "valor de promociones activas");
            if (temperatura == null || temperatura < -50 || temperatura > 50) {
                throw new IllegalArgumentException("La temperatura debe estar entre -50 y 50 grados.");
            }
            if (historialVisitas == null || historialVisitas < 0) {
                throw new IllegalArgumentException("Las visitas previas no pueden ser negativas.");
            }
        }

        private static void exigir(List<String> validos, String valor, String que) {
            if (valor == null || !validos.contains(valor)) {
                throw new IllegalArgumentException(
                        "El " + que + " debe ser uno de: " + String.join(", ", validos) + ".");
            }
        }
    }

    /** Resultado para una hora concreta. */
    public record Resultado(int hora, boolean demandaAlta, double confianza) {

        public Map<String, Object> comoMapa() {
            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("hora", hora);
            mapa.put("demandaAlta", demandaAlta);
            mapa.put("confianza", confianza);
            return mapa;
        }
    }

    public Resultado predecir(Condiciones condiciones, int hora) {
        condiciones.validar();
        if (hora < 0 || hora > 23) {
            throw new IllegalArgumentException("La hora debe estar entre 0 y 23.");
        }
        try {
            Instance instancia = construirInstancia(condiciones, hora);
            double indice = clasificador.classifyInstance(instancia);
            boolean alta = "Si".equals(estructura.classAttribute().value((int) indice));
            double confianza = clasificador.distributionForInstance(instancia)[(int) indice];
            return new Resultado(hora, alta, Math.round(confianza * 1000) / 10.0);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular la prediccion: " + e.getMessage(), e);
        }
    }

    /** Recorre el horario hora a hora para ver en que franjas se espera mas afluencia. */
    public List<Resultado> previsionDelDia(Condiciones condiciones, Integer desde, Integer hasta) {
        int inicio = desde == null ? HORA_APERTURA : desde;
        int fin = hasta == null ? HORA_CIERRE : hasta;
        if (inicio < 0 || fin > 23 || inicio > fin) {
            throw new IllegalArgumentException("El rango horario debe estar entre 0 y 23.");
        }
        List<Resultado> franjas = new ArrayList<>();
        for (int hora = inicio; hora <= fin; hora++) {
            franjas.add(predecir(condiciones, hora));
        }
        return franjas;
    }

    private Instance construirInstancia(Condiciones c, int hora) {
        Instance instancia = new DenseInstance(estructura.numAttributes());
        instancia.setDataset(estructura);
        instancia.setValue(estructura.attribute("Dia_Semana"), c.diaSemana());
        instancia.setValue(estructura.attribute("Hora"), hora);
        instancia.setValue(estructura.attribute("Clima"), c.clima());
        instancia.setValue(estructura.attribute("Temperatura"), c.temperatura());
        instancia.setValue(estructura.attribute("Historial_Visitas"), c.historialVisitas());
        instancia.setValue(estructura.attribute("Promociones_Activas"), c.promocionesActivas());
        return instancia;
    }
}
