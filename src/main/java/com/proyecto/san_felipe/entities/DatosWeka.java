package com.proyecto.san_felipe.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "datos_weka")
public class DatosWeka {

    @Id
    private String id;
    private Integer idCliente;
    private String diaSemana;
    private Double hora;
    private String clima;
    private Double temperatura;
    private String tipoServicio;
    private Integer historialVisitas;
    private String promocionesActivas;
    private String prediccion;
    private String confianza;

    public DatosWeka() {}

    public DatosWeka(Integer idCliente, String diaSemana, Double hora, String clima,
                     Double temperatura, String tipoServicio, Integer historialVisitas,
                     String promocionesActivas, String prediccion, String confianza) {
        this.idCliente = idCliente;
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

    public Integer getIdCliente() { return idCliente; }
    public void setIdCliente(Integer idCliente) { this.idCliente = idCliente; }

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
    public void setConfianza(String confianza) { this.confianza = confianza; }
}