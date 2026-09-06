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
    public List<Car> findALl(@RequestParam(value = "clientId", required = false) String clientId) {
        // Con ?clientId=... se obtienen solo los vehiculos de ese cliente.
        return clientId == null || clientId.isBlank()
                ? carService.getAllCars()
                : carService.getCarsByClient(clientId);
    }


    @PostMapping("/register")
    public ResponseEntity<Car> registerCar(@RequestBody Car car) {
        Car savedCar = carService.registerCar(car);
        return new ResponseEntity<>(savedCar, HttpStatus.CREATED);
    }


    @DeleteMapping("/{licencePlate}")
    public ResponseEntity<String> deleteCar(@PathVariable("licencePlate")String licencePlate) {
        carService.deleteCarByLicencePlate(licencePlate);
        return ResponseEntity.ok("car deleted successfully with licence plate: " + licencePlate);
    }


    @GetMapping("/{licencePlate}")
    public ResponseEntity<Car> findCarByLicencePlate(@PathVariable("licencePlate") String licencePlate) {
        Car car = carService.getCarByLicencePlate(licencePlate);
        if (car == null) {
            // El listado de vehiculos identifica por id, no por placa.
            car = carService.getCarByIdOrNull(licencePlate);
        }
        if (car != null) {
            return ResponseEntity.ok(car);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }


    }

    @PutMapping("/{identificador}")
    public ResponseEntity<Car> updateCar(@PathVariable("identificador") String identificador,
                                         @RequestBody Car car) {
        return ResponseEntity.ok(carService.updateCar(identificador, car));
    }
}
