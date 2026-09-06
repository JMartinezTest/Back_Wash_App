package com.proyecto.san_felipe.Controllers;

import com.proyecto.san_felipe.Services.clima.ClimaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/clima")
public class ClimaController {

    @Autowired
    private ClimaService climaService;

    /** Clima actual del lavadero, para precargar la pantalla de predicción. */
    @GetMapping
    public ResponseEntity<?> actual() {
        ClimaService.Clima clima = climaService.actual();
        if (clima == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "No se pudo consultar el clima en este momento."));
        }
        return ResponseEntity.ok(clima.comoMapa());
    }
}
