package com.proyecto.san_felipe.Controllers;

import com.proyecto.san_felipe.Repository.PaymentRecordRepository;
import com.proyecto.san_felipe.entities.PaymentRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/payment-records")
public class PaymentRecordController {

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @PostMapping("/save")
    public ResponseEntity<PaymentRecord> savePayment(@RequestBody PaymentRecord record) {
        record.setCalculatedAt(new Date());
        PaymentRecord saved = paymentRecordRepository.save(record);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PaymentRecord>> getAllPayments() {
        return ResponseEntity.ok(paymentRecordRepository.findAllByOrderByCalculatedAtDesc());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<PaymentRecord>> getByEmployee(@PathVariable String employeeId) {
        return ResponseEntity.ok(paymentRecordRepository.findByEmployeeIdOrderByCalculatedAtDesc(employeeId));
    }
}
