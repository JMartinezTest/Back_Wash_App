package com.proyecto.san_felipe.Services;

import com.proyecto.san_felipe.Repository.ClientRepository;
import com.proyecto.san_felipe.Repository.CarRepository;
import com.proyecto.san_felipe.Repository.EmployeeRepository;
import com.proyecto.san_felipe.entities.Count;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CountService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    public Count getCounts() {
        long clientsCount = clientRepository.count();
        long carsCount = carRepository.count();
        long employeesCount = employeeRepository.count();

        return new Count(employeesCount, clientsCount, carsCount);
    }
}

