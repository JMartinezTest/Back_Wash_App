package com.proyecto.san_felipe.Repository;

// import com.proyecto.san_felipe.entities.Client;
import com.proyecto.san_felipe.entities.Employee;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EmployeeRepository extends MongoRepository<Employee, String> {
    List<Employee> findByName(String name);
}
