package com.proyecto.san_felipe.Repository;

import com.proyecto.san_felipe.entities.DatosWeka;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DatosWekaRepository extends MongoRepository<DatosWeka, String> {
}