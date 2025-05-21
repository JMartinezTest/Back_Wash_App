package com.proyecto.san_felipe.Services;

import com.proyecto.san_felipe.Repository.EmployeeRepository;
import com.proyecto.san_felipe.entities.Car;
import com.proyecto.san_felipe.entities.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    public Employee registerEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

    public List<Employee> getAllEmployees(){
        return employeeRepository.findAll();
    }

    public void deleteEmployeeById(String id){
        Employee employee = employeeRepository.findById(id).orElse(null);
        if(employee!=null){
            employeeRepository.delete(employee);
        }else {
            throw new RuntimeException("el empleado " + id + " Not found");
        }
    }

}
