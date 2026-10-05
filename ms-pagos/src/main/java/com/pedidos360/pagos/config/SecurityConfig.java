package com.pedidos360.pagos.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    /**
     * Prueba local sin Azure: AUTH_BYPASS=true (via .env/docker-compose) permite
     * llamar a la API sin JWT. Por defecto false => autenticación completa.
     *
     * En producción ms-pagos exige un JWT válido: ms-carrito reenvía el token
     * del usuario cuando llama a POST /pagos.
     */
    @Value("${app.auth.bypass:false}")
    private boolean bypass;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS: permite al frontend (otro origen) llamar a esta API
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable());

        if (bypass) {
            // SOLO PRUEBA LOCAL: acceso abierto, sin JWT de Azure.
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        }

        return http.build();
    }

    /**
     * Origenes permitidos para el frontend:
     *  - http://localhost:4200 -> `npm start` (ng serve) en local
     *  - http://localhost      -> contenedor Docker del frontend (nginx, puerto 80)
     * Si se despliega en otro host (p. ej. EC2), anadir su origen aqui.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:4200",
            "http://localhost",
            "http://127.0.0.1*",
            "https://44.200.146.153"   // frontend desplegado (ver GUIA-DESPLIEGUE.txt)
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
