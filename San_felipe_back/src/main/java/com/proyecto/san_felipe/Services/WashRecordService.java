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
        // Validar que los servicios ofrecidos existan y calcular el total
        double total = 0.0;
        if (washRecord.getServicesOffered() != null) {
            for (String serviceId : washRecord.getServicesOffered()) {
                Optional<ServiceOffered> service = serviceOfferedRepository.findById(serviceId);
                if (service.isEmpty()) {
                    throw new IllegalArgumentException("El servicio ofrecido con ID " + serviceId + " no existe.");
                }
                total += service.get().getPrice();
            }
        }

        washRecord.setTotalPrice(total);
        washRecord.setDate(new Date());
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
            return 0.0;
        }
        double totalPayment = records.stream()
                .mapToDouble(WashRecord::getTotalPrice)
                .sum();
        return totalPayment * 0.35; // Aplica el 35% al total
    }
}
