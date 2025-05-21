package com.proyecto.san_felipe.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "datos_weka")
public class DatosWeka {

    @Id
    private String id;

    private String diaSemana;           // {Lunes, Martes, ..., Domingo}
    private String jornada;             // {Mañana, Tarde, Noche}
    private String clima;               // {Soleado, Lluvioso, Nublado}
    private Double temperatura;         // NUMERIC
    private String tipoServicio;        // {Basico, Completo, Premium}
    private Integer historialVisitas;  // NUMERIC
    private String promocionesActivas; // {Si, No}
    private String clientesEstimados;  // {Baja, Media, Alta}

    public DatosWeka() {}

    public DatosWeka(String diaSemana, String jornada, String clima,
                     Double temperatura, String tipoServicio, Integer historialVisitas,
                     String promocionesActivas, String clientesEstimados) {
        this.diaSemana = diaSemana;
        this.jornada = jornada;
        this.clima = clima;
        this.temperatura = temperatura;
        this.tipoServicio = tipoServicio;
        this.historialVisitas = historialVisitas;
        this.promocionesActivas = promocionesActivas;
        this.clientesEstimados = clientesEstimados;
    }

    // Getters y Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(String diaSemana) {
        this.diaSemana = diaSemana;
    }

    public String getJornada() {
        return jornada;
    }

    public void setJornada(String jornada) {
        this.jornada = jornada;
    }

    public String getClima() {
        return clima;
    }

    public void setClima(String clima) {
        this.clima = clima;
    }

    public Double getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(Double temperatura) {
        this.temperatura = temperatura;
    }

    public String getTipoServicio() {
        return tipoServicio;
    }

    public void setTipoServicio(String tipoServicio) {
        this.tipoServicio = tipoServicio;
    }

    public Integer getHistorialVisitas() {
        return historialVisitas;
    }

    public void setHistorialVisitas(Integer historialVisitas) {
        this.historialVisitas = historialVisitas;
    }

    public String getPromocionesActivas() {
        return promocionesActivas;
    }

    public void setPromocionesActivas(String promocionesActivas) {
        this.promocionesActivas = promocionesActivas;
    }

    public String getClientesEstimados() {
        return clientesEstimados;
    }

    public void setClientesEstimados(String clientesEstimados) {
        this.clientesEstimados = clientesEstimados;
    }
}
