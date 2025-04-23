package com.proyecto.san_felipe.Repository;



import com.proyecto.san_felipe.entities.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, Long> {
    Optional<User> findByUsername(String username);  // Define el método personalizado para buscar por nombre de usuario
}