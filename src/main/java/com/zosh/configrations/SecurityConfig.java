// =============================================================================
// USER SERVICE - SecurityConfig CORREGIDO para permitir Feign calls
// src/main/java/com/zosh/configrations/SecurityConfig.java
// =============================================================================
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
        System.out.println("🔧 USER SERVICE - CONFIGURANDO SECURITY PARA FEIGN CALLS");

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .authorizeHttpRequests(auth -> {
                    System.out.println("🔧 PERMITIENDO TODAS LAS REQUESTS PARA DEBUGGING");
                    auth
                            // Permitir endpoints específicos para Feign Client
                            .requestMatchers("/api/users/**").permitAll()
                            .requestMatchers("/api/users/profile").permitAll()
                            .requestMatchers("/api/users/by-cognito-id/**").permitAll()
                            .requestMatchers("/api/users/create-from-cognito").permitAll()
                            .requestMatchers("/api/users/**/upgrade-to-salon-owner").permitAll()
                            .requestMatchers("/api/users/**/has-salon").permitAll()

                            // Permitir endpoints de health y actuator
                            .requestMatchers("/actuator/**").permitAll()
                            .requestMatchers("/health").permitAll()

                            // Para desarrollo: permitir todo temporalmente
                            .anyRequest().permitAll();
                });

        System.out.println("✅ USER SERVICE - SECURITY CONFIGURADO SIN RESTRICCIONES PARA DEBUGGING");
        return http.build();
    }
}