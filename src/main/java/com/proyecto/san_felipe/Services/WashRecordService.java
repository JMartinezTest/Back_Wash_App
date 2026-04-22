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
        if (washRecord.getServiceOffered() == null || washRecord.getServiceOffered().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un servicio.");
        }
        for (String serviceId : washRecord.getServiceOffered()) {
            if (serviceOfferedRepository.findById(serviceId).isEmpty()) {
                throw new IllegalArgumentException("El servicio con ID " + serviceId + " no existe.");
            }
        }
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
