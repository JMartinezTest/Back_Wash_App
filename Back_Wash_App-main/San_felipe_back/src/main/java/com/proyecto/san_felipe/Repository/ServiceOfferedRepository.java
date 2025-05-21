package com.proyecto.san_felipe.Repository;

import com.proyecto.san_felipe.entities.ServiceOffered;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
public interface ServiceOfferedRepository extends MongoRepository<ServiceOffered, String> {
    List<ServiceOffered> findByName(String name);


}
