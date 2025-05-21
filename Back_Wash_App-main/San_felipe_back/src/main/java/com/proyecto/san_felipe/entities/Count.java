package com.proyecto.san_felipe.entities;

public class Count {
    private long employeesCount;
    private long clientsCount;
    private long carsCount;

    public Count(long employeesCount, long clientsCount, long carsCount) {
        this.employeesCount = employeesCount;
        this.clientsCount = clientsCount;
        this.carsCount = carsCount;
    }

    public long getEmployeesCount() {
        return employeesCount;
    }

    public void setEmployeesCount(long employeesCount) {
        this.employeesCount = employeesCount;
    }

    public long getClientsCount() {
        return clientsCount;
    }

    public void setClientsCount(long clientsCount) {
        this.clientsCount = clientsCount;
    }

    public long getCarsCount() {
        return carsCount;
    }

    public void setCarsCount(long carsCount) {
        this.carsCount = carsCount;
    }
}
