package com.proyecto.san_felipe.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document
public class WashRecord {

    @Id
    private String id;
    private Date date;
    private String client;
    private String employee;
    private String car;
    private List<String> serviceOffered;
    private double total;

    /**
     * Condiciones meteorologicas en el momento del lavado. Se rellenan solas desde
     * el servicio de clima: son dos de las variables del modelo de demanda, y sin
     * ellas el historial no sirve para reentrenarlo con datos propios.
     */
    private String clima;
    private Double temperatura;

    public WashRecord() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getClient() { return client; }
    public void setClient(String client) { this.client = client; }

    public String getEmployee() { return employee; }
    public void setEmployee(String employee) { this.employee = employee; }

    public String getCar() { return car; }
    public void setCar(String car) { this.car = car; }

    public List<String> getServiceOffered() { return serviceOffered; }
    public void setServiceOffered(List<String> serviceOffered) { this.serviceOffered = serviceOffered; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getClima() { return clima; }
    public void setClima(String clima) { this.clima = clima; }
    public Double getTemperatura() { return temperatura; }
    public void setTemperatura(Double temperatura) { this.temperatura = temperatura; }
}
