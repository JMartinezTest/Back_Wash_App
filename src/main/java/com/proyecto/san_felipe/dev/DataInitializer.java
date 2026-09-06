package com.proyecto.san_felipe.dev;

import com.proyecto.san_felipe.Repository.UserRepository;
import com.proyecto.san_felipe.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    /**
     * Credenciales de la primera cuenta, solo se usan si no hay ningun usuario.
     *
     * La contrasenia se toma del entorno: antes estaba escrita en el codigo y el
     * repositorio es publico, asi que cualquiera podia leerla.
     */
    @Value("${admin.usuario:admin}")
    private String usuarioInicial;

    @Value("${admin.password:}")
    private String passwordInicial;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            System.out.println("La base de datos ya contiene usuarios, no se crearon datos iniciales.");
            return;
        }

        if (passwordInicial == null || passwordInicial.isBlank()) {
            System.out.println("""
                    ATENCION: no hay usuarios y no se ha definido ADMIN_PASSWORD.
                    No se creo ninguna cuenta: nadie podra entrar a la aplicacion.
                    Define ADMIN_PASSWORD y reinicia para crear la cuenta inicial.""");
            return;
        }

        User admin = new User();
        admin.setUsername(usuarioInicial);
        admin.setPassword(passwordEncoder.encode(passwordInicial));
        admin.setRole("ADMIN");
        userRepository.save(admin);
        System.out.println("Usuario administrador creado: username=" + usuarioInicial);
    }
}
