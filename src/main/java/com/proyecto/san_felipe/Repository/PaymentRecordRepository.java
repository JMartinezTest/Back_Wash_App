package com.proyecto.san_felipe.Repository;

import com.proyecto.san_felipe.entities.PaymentRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PaymentRecordRepository extends MongoRepository<PaymentRecord, String> {
    List<PaymentRecord> findByEmployeeIdOrderByCalculatedAtDesc(String employeeId);
    List<PaymentRecord> findAllByOrderByCalculatedAtDesc();
}
