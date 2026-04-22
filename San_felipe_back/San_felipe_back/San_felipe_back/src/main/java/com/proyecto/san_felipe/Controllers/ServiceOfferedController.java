package com.proyecto.san_felipe.Controllers;

import com.proyecto.san_felipe.Services.ServiceOfferedService;
import com.proyecto.san_felipe.entities.ServiceOffered;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/services")
public class ServiceOfferedController {

    @Autowired
    private ServiceOfferedService serviceOfferedService;

    @GetMapping
    public List<ServiceOffered> findAll() {
        return serviceOfferedService.getAllServicesOffered();
    }

    @PostMapping("/register")
    public ResponseEntity<ServiceOffered> registerServiceOffered(@RequestBody ServiceOffered serviceOffered) {
        ServiceOffered savedServiceOffered = serviceOfferedService.registerServiceOffered(serviceOffered);
        return new ResponseEntity<>(savedServiceOffered, HttpStatus.CREATED);
    }
}
