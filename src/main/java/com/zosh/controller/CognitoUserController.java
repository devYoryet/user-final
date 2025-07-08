package com.zosh.controller;

import com.zosh.domain.UserRole;
import com.zosh.modal.User;
import com.zosh.payload.dto.CognitoUserInfo;
import com.zosh.payload.request.UpdateRoleRequest;
import com.zosh.repository.UserRepository;
import com.zosh.service.CognitoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class CognitoUserController {

    private final CognitoService cognitoService;
    private final UserRepository userRepository;

    /**
     * Actualiza el rol del usuario en Cognito y en la base de datos local
     */
    @PostMapping("/update-cognito-role")
    public ResponseEntity<?> updateUserRole(
            @RequestHeader("Authorization") String authToken,
            @RequestBody UpdateRoleRequest request) {
        try {
            // Extraer información del JWT
            CognitoUserInfo userInfo = cognitoService.extractUserInfoFromJWT(authToken);

            // Actualizar en Cognito
            cognitoService.updateUserAttribute(userInfo.getSub(), "custom:role", request.getRole());

            // Actualizar en base de datos local
            User user = userRepository.findByEmail(userInfo.getEmail());
            if (user != null) {
                user.setRole(UserRole.valueOf(request.getRole()));
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Role updated successfully",
                    "newRole", request.getRole()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to update role: " + e.getMessage()));
        }
    }

    /**
     * Sincroniza un usuario de Cognito con la base de datos local
     */
    @PostMapping("/sync-cognito-user")
    public ResponseEntity<?> syncCognitoUser(@RequestHeader("Authorization") String authToken) {
        try {
            CognitoUserInfo userInfo = cognitoService.extractUserInfoFromJWT(authToken);

            User user = userRepository.findByEmail(userInfo.getEmail());
            if (user == null) {
                // Crear nuevo usuario
                user = new User();
                user.setEmail(userInfo.getEmail());
                user.setFullName(userInfo.getName() != null ? userInfo.getName() : userInfo.getEmail());
                user.setUsername(userInfo.getPreferredUsername() != null ? userInfo.getPreferredUsername()
                        : userInfo.getEmail().split("@")[0]);
                user.setRole(UserRole.valueOf(userInfo.getCustomRole()));
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                user = userRepository.save(user);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "User synchronized successfully",
                    "userId", user.getId(),
                    "role", user.getRole()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to sync user: " + e.getMessage()));
        }
    }
}