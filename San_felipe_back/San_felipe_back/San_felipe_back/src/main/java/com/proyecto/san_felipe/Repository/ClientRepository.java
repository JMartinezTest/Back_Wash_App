package com.proyecto.san_felipe.Repository;

import com.proyecto.san_felipe.entities.Client;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
public interface ClientRepository extends MongoRepository<Client, String> {
    List<Client> findByName(String name);
}
