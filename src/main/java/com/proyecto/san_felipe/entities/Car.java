package com.proyecto.san_felipe.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document
public class Car {
    @Id
    private  String id;
    @Field
    private String licencePlate;
    @Field
    private String make;
    @Field
    private String color;

    /**
     * Cliente propietario del vehiculo.
     *
     * Se guarda el id como texto, igual que hacen los demas documentos (WashRecord
     * referencia asi a cliente, empleado y vehiculo). Antes habia aqui un @DBRef a
     * Client sin getter ni setter, asi que nunca llego a guardarse: los vehiculos
     * creados antes de este cambio tienen el campo vacio.
     */
    @Field
    private String clientId;

    public Car() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLicencePlate() {
        return licencePlate;
    }

    public void setLicencePlate(String licencePlate) {
        this.licencePlate = licencePlate;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
