package com.proyecto.san_felipe.Services;

import com.proyecto.san_felipe.Repository.ServiceOfferedRepository;
import com.proyecto.san_felipe.Repository.WashRecordRepository;
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

    public WashRecord registerWashRecord(WashRecord washRecord) {
    List<String> serviceIds = washRecord.getServiceOffered();
    if (serviceIds == null || serviceIds.isEmpty()) {
        throw new IllegalArgumentException("Debe especificar al menos un servicio ofrecido.");
    }

    for (String serviceId : serviceIds) {
        Optional<ServiceOffered> service = serviceOfferedRepository.findById(serviceId);
        if (service.isEmpty()) {
            throw new IllegalArgumentException("El servicio ofrecido con ID " + serviceId + " no existe.");
        }
    }

    Date now = new Date();
    washRecord.setDate(now);
    System.out.println("Fecha asignada: " + now);
    return washRecordRepository.save(washRecord);
}


    public List<WashRecord> getAllWashRecord() {
        return washRecordRepository.findAll();
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
        System.out.println("No hay registros para el rango de fechas");
        return 0.0;
    }

    double totalPayment = 0;

    for (WashRecord record : records) {
        List<String> serviceIds = record.getServiceOffered();
        if (serviceIds != null) {
            for (String serviceId : serviceIds) {
                Optional<ServiceOffered> service = serviceOfferedRepository.findById(serviceId);
                if (service.isPresent()) {
                    totalPayment += service.get().getPrice();
                } else {
                    System.err.println("Servicio con ID " + serviceId + " no encontrado.");
                }
            }
        }
    }

    return totalPayment * 0.35; // Aplica el 35% al total
}

}
