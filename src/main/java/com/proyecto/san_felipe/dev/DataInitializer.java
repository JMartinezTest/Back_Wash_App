package com.proyecto.san_felipe.dev;

import com.proyecto.san_felipe.entities.User;
import com.proyecto.san_felipe.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    // Create BCryptPasswordEncoder instance directly instead of autowiring
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) throws Exception {
        // Verificar si ya existe un usuario en la base de datos
        if (userRepository.count() == 0) {
            // Crear un usuario administrador por defecto
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123")); // Contraseña encriptada
            admin.setRole("ADMIN");

            // Guardar el usuario en la base de datos
            userRepository.save(admin);
            System.out.println("Usuario administrador creado con éxito: username=admin, role=ADMIN");
        } else {
            System.out.println("La base de datos ya contiene usuarios, no se crearon datos iniciales.");
        }
    }
}