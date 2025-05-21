package com.proyecto.san_felipe.Services;

import com.proyecto.san_felipe.Repository.ServiceOfferedRepository;
import com.proyecto.san_felipe.entities.Client;
import com.proyecto.san_felipe.entities.ServiceOffered;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ServiceOfferedService {

    @Autowired
    private ServiceOfferedRepository serviceOfferedRepository;

    public ServiceOffered registerServiceOffered(ServiceOffered serviceOffered) {
        return serviceOfferedRepository.save(serviceOffered);
    }

    public List<ServiceOffered> getAllServicesOffered() {
        return serviceOfferedRepository.findAll();
    }

    public void deleteServiceById(String id){
        Optional<ServiceOffered> service = serviceOfferedRepository.findById(id);
        if (service.isPresent()){
            serviceOfferedRepository.deleteById(id);
        }else {
            throw new RuntimeException("Service " + id + " Not found");
        }
    }
}
