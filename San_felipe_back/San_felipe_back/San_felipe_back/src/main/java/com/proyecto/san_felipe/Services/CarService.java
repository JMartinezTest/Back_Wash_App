package com.proyecto.san_felipe.Services;

import com.proyecto.san_felipe.Repository.CarRepository;
import com.proyecto.san_felipe.entities.Car;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarService {
    @Autowired
    private CarRepository carRepository;

    public Car registerCar(Car car) {
        return carRepository.save(car);
    }

    public List<Car> getAllCars(){
        return carRepository.findAll();
    }

    public void deleteCarByLicencePlate(String licensePlate){
        Car car = carRepository.findByLicencePlate(licensePlate);
        if (car!= null){
            carRepository.delete(car);
        }else {
            throw new RuntimeException("Licence plate " + licensePlate + " Not found");
        }
    }


    public Car getCarByLicencePlate(String licencePlate) {
        return carRepository.findByLicencePlate(licencePlate);
    }


}
