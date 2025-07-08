// =============================================================================
// USER SERVICE - UserController MEJORADO con headers del Gateway
// src/main/java/com/zosh/controller/UserController.java
// =============================================================================
package com.zosh.controller;

import com.zosh.exception.UserException;
import com.zosh.mapper.UserMapper;
import com.zosh.modal.User;
import com.zosh.payload.dto.UserDTO;
import com.zosh.payload.request.CreateUserFromCognitoRequest;
import com.zosh.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final UserMapper userMapper;

	// =========================================================================
	// MÉTODO PRINCIPAL CON SOPORTE COMPLETO PARA HEADERS DEL GATEWAY
	// =========================================================================

	@GetMapping("/api/users/profile")
	public ResponseEntity<UserDTO> getUserFromJwtToken(
			HttpServletRequest request,
			@RequestHeader("Authorization") String jwt,
			@RequestHeader(value = "X-Cognito-Sub", required = false) String cognitoSub,
			@RequestHeader(value = "X-User-Email", required = false) String userEmail,
			@RequestHeader(value = "X-User-Username", required = false) String username,
			@RequestHeader(value = "X-User-Role", required = false) String userRole,
			@RequestHeader(value = "X-Auth-Source", required = false) String authSource) throws Exception {

		System.out.println("🔍 =========================");
		System.out.println("🔍 USER SERVICE - PROFILE REQUEST");
		System.out.println("🔍 =========================");

		// Debug headers específicos
		System.out.println("🎯 HEADERS DEL GATEWAY:");
		System.out.println("   X-Cognito-Sub: " + cognitoSub);
		System.out.println("   X-User-Email: " + userEmail);
		System.out.println("   X-User-Username: " + username);
		System.out.println("   X-User-Role: " + userRole);
		System.out.println("   X-Auth-Source: " + authSource);

		if ("Cognito".equals(authSource) && cognitoSub != null && !cognitoSub.isEmpty()) {
			// ✅ Usuario procesado por Gateway desde Cognito
			System.out.println("✅ PROCESANDO USUARIO DE COGNITO VIA GATEWAY");
			return handleCognitoUserFromGateway(cognitoSub, userEmail, username, userRole);
		} else {
			// 🔄 Sistema JWT anterior (Keycloak o directo)
			System.out.println("🔄 PROCESANDO CON SISTEMA JWT ANTERIOR");
			try {
				User user = userService.getUserFromJwtToken(jwt);
				if (user != null) {
					UserDTO userDTO = userMapper.mapToDTO(user);
					return new ResponseEntity<>(userDTO, HttpStatus.OK);
				} else {
					throw new UserException("Usuario no encontrado con JWT");
				}
			} catch (Exception e) {
				System.err.println("❌ Error con JWT tradicional: " + e.getMessage());
				throw new UserException("Error procesando autenticación: " + e.getMessage());
			}
		}
	}

	private ResponseEntity<UserDTO> handleCognitoUserFromGateway(
			String cognitoSub, String email, String username, String role) {
		try {
			System.out.println("🔍 PROCESANDO USUARIO DE COGNITO:");
			System.out.println("   Sub: " + cognitoSub);
			System.out.println("   Email: " + email);
			System.out.println("   Username: " + username);
			System.out.println("   Role: " + role);

			// Normalizar datos
			String finalEmail = (email != null && !email.isEmpty()) ? email : username;
			String finalUsername = (username != null && !username.isEmpty()) ? username : finalEmail;
			String finalRole = (role != null && !role.isEmpty()) ? role : "SALON_OWNER"; // Default para become-partner

			// 1. Buscar por Cognito ID primero
			try {
				User user = userService.findByCognitoUserId(cognitoSub);
				System.out.println("✅ Usuario encontrado por Cognito ID: " + user.getId());
				UserDTO userDTO = userMapper.mapToDTO(user);
				return ResponseEntity.ok(userDTO);
			} catch (UserException e) {
				System.out.println("🔍 Usuario no encontrado por Cognito ID, buscando por email...");
			}

			// 2. Buscar por email si existe
			if (finalEmail != null && !finalEmail.contains("@cognito.generated")) {
				try {
					User user = userService.findByEmail(finalEmail);
					// Actualizar con Cognito ID
					user.setCognitoUserId(cognitoSub);
					userService.updateCognitoUserId(user.getId(), cognitoSub);

					System.out.println("✅ Usuario encontrado por email y actualizado: " + user.getId());
					UserDTO userDTO = userMapper.mapToDTO(user);
					return ResponseEntity.ok(userDTO);
				} catch (UserException e) {
					System.out.println("🔍 Usuario no encontrado por email, creando nuevo...");
				}
			}

			// 3. Crear nuevo usuario
			CreateUserFromCognitoRequest request = new CreateUserFromCognitoRequest();
			request.setCognitoUserId(cognitoSub);
			request.setEmail(finalEmail);
			request.setFullName(generateDisplayName(finalEmail, finalUsername, cognitoSub));
			request.setRole(finalRole);

			User newUser = userService.createUserFromCognito(request);
			UserDTO userDTO = userMapper.mapToDTO(newUser);

			System.out.println("✅ Usuario creado exitosamente: " + newUser.getId());
			return ResponseEntity.ok(userDTO);

		} catch (Exception e) {
			System.err.println("❌ Error procesando usuario de Cognito: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Error crítico procesando usuario: " + e.getMessage());
		}
	}

	private String generateDisplayName(String email, String username, String cognitoSub) {
		if (email != null && !email.contains("@cognito.generated")) {
			return email.split("@")[0]; // Parte antes del @
		} else if (username != null && !username.startsWith("user_")) {
			return username;
		} else {
			return "Usuario " + cognitoSub.substring(0, 8);
		}
	}

	// =========================================================================
	// MÉTODOS EXISTENTES
	// =========================================================================

	@GetMapping("/api/users/{userId}")
	public ResponseEntity<UserDTO> getUserById(@PathVariable Long userId) throws UserException {
		System.out.println("🔍 getUserById: " + userId);
		User user = userService.getUserById(userId);
		if (user == null) {
			throw new UserException("User not found");
		}
		UserDTO userDTO = userMapper.mapToDTO(user);
		return new ResponseEntity<>(userDTO, HttpStatus.OK);
	}

	@GetMapping("/api/users")
	public ResponseEntity<List<User>> getUsers() throws UserException {
		List<User> users = userService.getAllUsers();
		return new ResponseEntity<>(users, HttpStatus.OK);
	}

	// =========================================================================
	// MÉTODOS ADICIONALES PARA GATEWAY
	// =========================================================================

	@GetMapping("/api/users/by-cognito-id/{cognitoUserId}")
	public ResponseEntity<UserDTO> getUserByCognitoId(@PathVariable String cognitoUserId) {
		try {
			User user = userService.findByCognitoUserId(cognitoUserId);
			UserDTO userDTO = userMapper.mapToDTO(user);
			return ResponseEntity.ok(userDTO);
		} catch (Exception e) {
			return ResponseEntity.notFound().build();
		}
	}

	@PostMapping("/api/users/create-from-cognito")
	public ResponseEntity<UserDTO> createUserFromCognito(@RequestBody CreateUserFromCognitoRequest request) {
		try {
			System.out.println("🚀 Creando usuario desde request API:");
			System.out.println("   Request: " + request);

			User user = userService.createUserFromCognito(request);
			UserDTO userDTO = userMapper.mapToDTO(user);
			return ResponseEntity.ok(userDTO);
		} catch (Exception e) {
			System.err.println("❌ Error creando usuario: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
}