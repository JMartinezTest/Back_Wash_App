package com.proyecto.san_felipe.security;

import com.proyecto.san_felipe.Repository.UserRepository;
import com.proyecto.san_felipe.entities.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Service;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private AuthService authService;

    // Usa la misma KEY en todo el proyecto
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor("secretsecretsecretsecretsecretsecret".getBytes());

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .cors().and()
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()    // rutas públicas
                        .anyRequest().authenticated()                // el resto requiere auth
                )
                // Añadimos nuestro filtro **ANTES** de UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtTokenFilter jwtTokenFilter() {
        return new JwtTokenFilter(authService);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.addAllowedOrigin("http://localhost:3000");
        cfg.addAllowedMethod("*");
        cfg.addAllowedHeader("*");
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    // --- Filtro JWT interno ---
    public class JwtTokenFilter extends OncePerRequestFilter {

        private final AuthService authService;

        public JwtTokenFilter(AuthService authService) {
            this.authService = authService;
        }

        /**
         * Evita que el filtro JWT se aplique sobre /auth/**
         */
        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String path = request.getServletPath();
            return path.startsWith("/auth/");
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain chain)
                throws ServletException, IOException {

            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                try {
                    String username = Jwts.parserBuilder()
                            .setSigningKey(SECRET_KEY)
                            .build()
                            .parseClaimsJws(token)
                            .getBody()
                            .getSubject();

                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        User user = authService.getUserByUsername(username);
                        if (user != null) {
                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    }
                } catch (Exception ex) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token inválido o expirado");
                    return;
                }
            }

            chain.doFilter(request, response);
        }
    }

    // --- Servicio de Auth interno ---
    @Service
    public static class AuthService {

        @Autowired
        private UserRepository userRepository;

        private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        public String register(String username, String password) {
            User u = new User();
            u.setUsername(username);
            u.setPassword(passwordEncoder.encode(password));
            u.setRole("ADMIN");
            userRepository.save(u);
            return "User registered successfully";
        }

        public String login(String username, String password) throws Exception {
            Optional<User> opt = userRepository.findByUsername(username);
            if (opt.isEmpty() || !passwordEncoder.matches(password, opt.get().getPassword())) {
                throw new Exception("Credenciales inválidas");
            }
            return generateToken(opt.get());
        }

        public User getUserByUsername(String username) {
            return userRepository.findByUsername(username).orElse(null);
        }

        private String generateToken(User user) {
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", user.getRole());
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(user.getUsername())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24h
                    .signWith(SECRET_KEY)
                    .compact();
        }
    }
}
