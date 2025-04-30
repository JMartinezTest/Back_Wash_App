package com.proyecto.san_felipe.entities;

// import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document
public class WashRecord {

    @Id
    private String id;
    private Date date;
    private String employee;
    private String client;
    private String car;
    private List<String> servicesOffered;
    private double totalPrice;

    public WashRecord() {

    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmployee() {
        return employee;
    }

    public void setEmployee(String employee) {
        this.employee = employee;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getCar() {
        return car;
    }

    public void setCar(String car) {
        this.car = car;
    }

    public List<String> getServicesOffered() {
        return servicesOffered;
    }

    public void setServicesOffered(List<String> servicesOffered) {
        this.servicesOffered = servicesOffered;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}
