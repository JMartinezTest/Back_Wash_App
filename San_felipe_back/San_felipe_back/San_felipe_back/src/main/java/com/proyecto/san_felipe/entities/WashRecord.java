package com.proyecto.san_felipe.entities;

// import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document
public class WashRecord {

    @Id
    private String id;
    private Date date;
    private String employee;
    private String car;
    private String serviceOffered;

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

    public String getCar() {
        return car;
    }

    public void setCar(String car) {
        this.car = car;
    }

    public String getServiceOffered() {
        return serviceOffered;
    }

    public void setServiceOffered(String serviceOffered) {
        this.serviceOffered = serviceOffered;
    }
}
