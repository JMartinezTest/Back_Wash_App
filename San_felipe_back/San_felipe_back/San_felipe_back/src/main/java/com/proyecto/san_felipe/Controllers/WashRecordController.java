package com.proyecto.san_felipe.Controllers;

import com.proyecto.san_felipe.Services.WashRecordService;
import com.proyecto.san_felipe.entities.WashRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/washed")
public class WashRecordController {

    @Autowired
    private WashRecordService washRecordService;

    @GetMapping
    public List<WashRecord> findAll() {
        return washRecordService.getAllWashRecord();
    }

    @PostMapping("/register")
    public ResponseEntity<WashRecord> registerWashRecord(@RequestBody WashRecord washRecord) {
        WashRecord savedWashRecord = washRecordService.registerWashRecord(washRecord);
        return new ResponseEntity<>(savedWashRecord, HttpStatus.CREATED);
    }

    @GetMapping("/by_employees")
    public ResponseEntity<List<WashRecord>> getWashRecordByEmployeeAndDate(
            @RequestParam String employee,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate) {
        List<WashRecord> records = washRecordService.getWashRecordByEmployeeAndDate(employee, startDate, endDate);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/employee/{employee}/payment")
    public ResponseEntity<Double> calculatePayment(
            @PathVariable String employee,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate) {
        try {
            if (startDate.after(endDate)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(0.0); // Error: Fecha de inicio después de la fecha de fin
            }
            double payment = washRecordService.calculateEmployeePayment(employee, startDate, endDate);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            e.printStackTrace(); // Log de error para depuración
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

}
