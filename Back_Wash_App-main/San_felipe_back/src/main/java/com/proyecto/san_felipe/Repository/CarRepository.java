package com.proyecto.san_felipe.Repository;

import com.proyecto.san_felipe.entities.Car;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CarRepository extends MongoRepository<Car, String> {
    Car findByLicencePlate(String licencePlate);
}
