package com.proyecto.san_felipe.Controllers;

import com.proyecto.san_felipe.Services.CarService;
import com.proyecto.san_felipe.entities.Car;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cars")
public class CarController {

    @Autowired
    private CarService carService;

    @GetMapping
    public List<Car> findALl() {
        return carService.getAllCars();
    }


    @PostMapping("/register")
    public ResponseEntity<Car> registerCar(@RequestBody Car car) {
        Car savedCar = carService.registerCar(car);
        return new ResponseEntity<>(savedCar, HttpStatus.CREATED);
    }
    @GetMapping("/{licencePlate}")
    public ResponseEntity<Car> findCarByLicencePlate(@PathVariable("licencePlate") String licencePlate) {
        Car car = carService.getCarByLicencePlate(licencePlate);
        if (car != null) {
            return ResponseEntity.ok(car);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }

    }
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteWashRecord(@PathVariable("id") String id){
        carService.deleteCarById(id);
        return ResponseEntity.ok("Registro "+ id + " eliminado.");
    }
}


