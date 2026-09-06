package com.proyecto.san_felipe.Services;

import com.proyecto.san_felipe.Repository.EmployeeRepository;
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

    public Employee getEmployeeById(String id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El empleado " + id + " no existe."));
    }

    public Employee updateEmployee(String id, Employee datos) {
        Employee empleado = getEmployeeById(id);
        empleado.setName(datos.getName());
        empleado.setLastName(datos.getLastName());
        empleado.setPosition(datos.getPosition());
        empleado.setPhoneNumber(datos.getPhoneNumber());
        return employeeRepository.save(empleado);
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
