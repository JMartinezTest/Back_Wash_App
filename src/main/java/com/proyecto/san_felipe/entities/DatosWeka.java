package com.proyecto.san_felipe.entities;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "datos_weka")
public class DatosWeka {

    @Id
    private String id;
    private String diaSemana;
    private Double hora;
    private String clima;
    private Double temperatura;
    private String tipoServicio;
    private Integer historialVisitas;
    private String promocionesActivas;
    private String prediccion;
    private String confianza;

    /**
     * Momento en que se hizo la consulta. El dia de la semana no basta: sin esto
     * dos predicciones de "Lunes" hechas en semanas distintas son indistinguibles.
     */
    private Date creadoEn;

    public DatosWeka() {}

    public DatosWeka(String diaSemana, Double hora, String clima,
                     Double temperatura, String tipoServicio, Integer historialVisitas,
                     String promocionesActivas, String prediccion, String confianza) {
        this.diaSemana = diaSemana;
        this.hora = hora;
        this.clima = clima;
        this.temperatura = temperatura;
        this.tipoServicio = tipoServicio;
        this.historialVisitas = historialVisitas;
        this.promocionesActivas = promocionesActivas;
        this.prediccion = prediccion;
        this.confianza = confianza;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDiaSemana() { return diaSemana; }
    public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }

    public Double getHora() { return hora; }
    public void setHora(Double hora) { this.hora = hora; }

    public String getClima() { return clima; }
    public void setClima(String clima) { this.clima = clima; }

    public Double getTemperatura() { return temperatura; }
    public void setTemperatura(Double temperatura) { this.temperatura = temperatura; }

    public String getTipoServicio() { return tipoServicio; }
    public void setTipoServicio(String tipoServicio) { this.tipoServicio = tipoServicio; }

    public Integer getHistorialVisitas() { return historialVisitas; }
    public void setHistorialVisitas(Integer historialVisitas) { this.historialVisitas = historialVisitas; }

    public String getPromocionesActivas() { return promocionesActivas; }
    public void setPromocionesActivas(String promocionesActivas) { this.promocionesActivas = promocionesActivas; }

    public String getPrediccion() { return prediccion; }
    public void setPrediccion(String prediccion) { this.prediccion = prediccion; }

    public String getConfianza() { return confianza; }
    public Date getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Date creadoEn) { this.creadoEn = creadoEn; }
    public void setConfianza(String confianza) { this.confianza = confianza; }

    /** Solo para la previsión del día: rango horario a evaluar. No se persiste. */
    @org.springframework.data.annotation.Transient
    private Integer horaInicio;

    @org.springframework.data.annotation.Transient
    private Integer horaFin;

    public Integer getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(Integer horaInicio) {
        this.horaInicio = horaInicio;
    }

    public Integer getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(Integer horaFin) {
        this.horaFin = horaFin;
    }
}
