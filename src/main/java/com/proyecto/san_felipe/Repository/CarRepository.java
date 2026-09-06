package com.proyecto.san_felipe.Repository;

import com.proyecto.san_felipe.entities.Car;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CarRepository extends MongoRepository<Car, String> {
    Car findFirstByLicencePlate(String licencePlate);

    List<Car> findAllByLicencePlate(String licencePlate);

    List<Car> findByClientId(String clientId);
}
