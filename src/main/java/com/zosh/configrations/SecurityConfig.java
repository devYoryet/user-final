package com.zosh.configrations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("🔧 USER SERVICE - CONFIGURANDO SECURITY ULTRA-PERMISIVO");
        
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable()) // Desactivar CORS también
            .authorizeHttpRequests(auth -> {
                System.out.println("🔧 PERMITIENDO TODAS LAS REQUESTS");
                auth.anyRequest().permitAll();
            });

        System.out.println("✅ USER SERVICE - SECURITY CONFIGURADO SIN RESTRICCIONES");
        return http.build();
    }
}