package com.proyecto.san_felipe.Repository;

import com.proyecto.san_felipe.entities.WashRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface WashRecordRepository extends MongoRepository<WashRecord, String> {
    List<WashRecord> findByCarAndDateBetween(String car, Date startDate, Date endDate);
    List<WashRecord> findByCar(String car);
    List<WashRecord> findByEmployeeAndDateBetween(String employee, Date starDate, Date endDate);

}
