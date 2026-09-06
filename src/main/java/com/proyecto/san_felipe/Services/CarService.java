package com.proyecto.san_felipe.Services;

import com.proyecto.san_felipe.Repository.CarRepository;
import com.proyecto.san_felipe.Repository.ClientRepository;
import com.proyecto.san_felipe.entities.Car;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarService {
    @Autowired
    private CarRepository carRepository;

    @Autowired
    private ClientRepository clientRepository;

    public Car registerCar(Car car) {
        // La placa identifica al vehiculo en todo el sistema, asi que no puede repetirse.
        if (car.getLicencePlate() != null
                && !carRepository.findAllByLicencePlate(car.getLicencePlate()).isEmpty()) {
            throw new IllegalArgumentException(
                    "Ya existe un vehiculo con la placa " + car.getLicencePlate() + ".");
        }
        exigirClienteExistente(car.getClientId());
        return carRepository.save(car);
    }

    public List<Car> getCarsByClient(String clientId) {
        exigirClienteExistente(clientId);
        return carRepository.findByClientId(clientId);
    }

    /** Todo vehiculo pertenece a un cliente, y ese cliente tiene que existir. */
    private void exigirClienteExistente(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el cliente propietario del vehiculo.");
        }
        if (clientRepository.findById(clientId).isEmpty()) {
            throw new IllegalArgumentException("El cliente " + clientId + " no existe.");
        }
    }

    public List<Car> getAllCars(){
        return carRepository.findAll();
    }

    public void deleteCarByLicencePlate(String identificador){
        carRepository.delete(getCar(identificador));
    }

    /**
     * Busca por placa y, si no hay coincidencia, por id de Mongo. El listado de vehiculos
     * identifica por id y el formulario de edicion por placa, asi que se aceptan las dos.
     */
    public Car getCar(String identificador) {
        Car car = carRepository.findFirstByLicencePlate(identificador);
        if (car == null) {
            car = carRepository.findById(identificador).orElse(null);
        }
        if (car == null) {
            throw new RuntimeException("No existe ningun vehiculo con placa o id " + identificador);
        }
        return car;
    }

    /** Devuelve null en lugar de fallar, para que el controller pueda responder 404. */
    public Car getCarByIdOrNull(String id) {
        return carRepository.findById(id).orElse(null);
    }

    public Car updateCar(String identificador, Car datos) {
        Car car = getCar(identificador);
        exigirClienteExistente(datos.getClientId());
        car.setLicencePlate(datos.getLicencePlate());
        car.setMake(datos.getMake());
        car.setColor(datos.getColor());
        car.setClientId(datos.getClientId());
        return carRepository.save(car);
    }


    public Car getCarByLicencePlate(String licencePlate) {
        return carRepository.findFirstByLicencePlate(licencePlate);
    }


}
