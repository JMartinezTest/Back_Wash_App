package com.proyecto.san_felipe.Services;

import com.proyecto.san_felipe.Repository.CarRepository;
import com.proyecto.san_felipe.Services.clima.ClimaService;
import com.proyecto.san_felipe.Repository.ServiceOfferedRepository;
import com.proyecto.san_felipe.Repository.WashRecordRepository;
import com.proyecto.san_felipe.entities.Car;
import com.proyecto.san_felipe.entities.ServiceOffered;
import com.proyecto.san_felipe.entities.WashRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class WashRecordService {

    @Autowired
    private ServiceOfferedRepository serviceOfferedRepository;

    @Autowired
    private WashRecordRepository washRecordRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private ClimaService climaService;


    /**
     * Un lavado no puede mezclar el coche de un cliente con otro distinto.
     *
     * Los vehiculos creados antes de que existiera la relacion no tienen dueño;
     * en ese caso se les asigna el cliente del lavado en lugar de rechazarlo, para
     * que los datos antiguos se vayan corrigiendo solos.
     */
    private void verificarPropietario(String idCliente, String idVehiculo) {
        if (idCliente == null || idCliente.isBlank() || idVehiculo == null || idVehiculo.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el cliente y el vehiculo del lavado.");
        }
        Car vehiculo = carRepository.findById(idVehiculo)
                .orElseThrow(() -> new IllegalArgumentException("El vehiculo " + idVehiculo + " no existe."));

        if (vehiculo.getClientId() == null || vehiculo.getClientId().isBlank()) {
            vehiculo.setClientId(idCliente);
            carRepository.save(vehiculo);
            return;
        }
        if (!vehiculo.getClientId().equals(idCliente)) {
            throw new IllegalArgumentException("El vehiculo con placa "
                    + vehiculo.getLicencePlate() + " no pertenece al cliente indicado.");
        }
    }

    public WashRecord registerWashRecord(WashRecord washRecord) {
        verificarPropietario(washRecord.getClient(), washRecord.getCar());
        if (washRecord.getServiceOffered() == null || washRecord.getServiceOffered().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un servicio.");
        }
        for (String serviceId : washRecord.getServiceOffered()) {
            if (serviceOfferedRepository.findById(serviceId).isEmpty()) {
                throw new IllegalArgumentException("El servicio con ID " + serviceId + " no existe.");
            }
        }
        washRecord.setDate(new Date());
        anotarClima(washRecord);
        return washRecordRepository.save(washRecord);
    }

    /**
     * Anota el tiempo que hacia al registrar el lavado. Si el servicio externo no
     * responde, el lavado se guarda igual: la operacion del negocio no puede
     * depender de una API de terceros.
     */
    private void anotarClima(WashRecord lavado) {
        if (lavado.getClima() != null && lavado.getTemperatura() != null) {
            return;  // Ya venian informados desde el cliente.
        }
        ClimaService.Clima clima = climaService.actual();
        if (clima != null) {
            lavado.setClima(clima.getDescripcion());
            lavado.setTemperatura(clima.getTemperatura());
        }
    }

    public List<WashRecord> getAllWashRecord() {
        return washRecordRepository.findAll();
    }

    public WashRecord getWashRecordById(String id) {
        return washRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El lavado " + id + " no existe."));
    }

    /** Actualiza un lavado conservando su fecha original. */
    public WashRecord updateWashRecord(String id, WashRecord datos) {
        WashRecord lavado = getWashRecordById(id);
        verificarPropietario(datos.getClient(), datos.getCar());
        if (datos.getServiceOffered() == null || datos.getServiceOffered().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un servicio.");
        }
        for (String serviceId : datos.getServiceOffered()) {
            if (serviceOfferedRepository.findById(serviceId).isEmpty()) {
                throw new IllegalArgumentException("El servicio con ID " + serviceId + " no existe.");
            }
        }
        lavado.setClient(datos.getClient());
        lavado.setEmployee(datos.getEmployee());
        lavado.setCar(datos.getCar());
        lavado.setServiceOffered(datos.getServiceOffered());
        lavado.setTotal(datos.getTotal());
        if (datos.getDate() != null) {
            lavado.setDate(datos.getDate());
        }
        return washRecordRepository.save(lavado);
    }

    public void deleteWashRecordById(String id) {
        washRecordRepository.delete(getWashRecordById(id));
    }

    public List<WashRecord> getWashRecordByCarAndTheRange(String car, Date startDate, Date endDate) {
        return washRecordRepository.findByCarAndDateBetween(car, startDate, endDate);
    }

    public List<WashRecord> getWashRecordByLicencePlate(String car) {
        return washRecordRepository.findByCar(car);
    }

    public List<WashRecord> getWashRecordByEmployeeAndDate(String employee, Date startDate, Date endDate) {
        return washRecordRepository.findByEmployeeAndDateBetween(employee, startDate, endDate);
    }

    public double calculateEmployeePayment(String employee, Date startDate, Date endDate) {
        List<WashRecord> records = washRecordRepository.findByEmployeeAndDateBetween(employee, startDate, endDate);

        if (records.isEmpty()) {
            return 0.0;
        }

        double totalPayment = 0;
        for (WashRecord record : records) {
            if (record.getServiceOffered() != null) {
                for (String serviceId : record.getServiceOffered()) {
                    Optional<ServiceOffered> service = serviceOfferedRepository.findById(serviceId);
                    if (service.isPresent()) {
                        totalPayment += service.get().getPrice();
                    } else {
                        System.err.println("Servicio con ID " + serviceId + " no encontrado.");
                    }
                }
            }
        }

        return totalPayment * 0.35;
    }
}
