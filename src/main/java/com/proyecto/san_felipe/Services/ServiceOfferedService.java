package com.proyecto.san_felipe.Services;

import com.proyecto.san_felipe.Repository.ServiceOfferedRepository;
import com.proyecto.san_felipe.entities.ServiceOffered;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public ServiceOffered getServiceOfferedById(String id) {
        return serviceOfferedRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El servicio " + id + " no existe."));
    }

    public ServiceOffered updateServiceOffered(String id, ServiceOffered datos) {
        ServiceOffered servicio = getServiceOfferedById(id);
        servicio.setName(datos.getName());
        servicio.setDescription(datos.getDescription());
        servicio.setPrice(datos.getPrice());
        servicio.setDuration(datos.getDuration());
        return serviceOfferedRepository.save(servicio);
    }

    public void deleteServiceOfferedById(String id) {
        serviceOfferedRepository.delete(getServiceOfferedById(id));
    }
}
