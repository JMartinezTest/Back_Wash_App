package com.proyecto.san_felipe.Controllers;

import com.proyecto.san_felipe.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private SecurityConfig.AuthService authService;

    @GetMapping()
    public String hello() {
        return "respuesta cierta";
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping("/register")
    public String register(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        System.out.println(password);
        System.out.println(username);
        return authService.register(username, password);
    }

    // @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) throws Exception {
        return authService.login(username, password);
    }
}
