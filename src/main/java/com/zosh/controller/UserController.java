// =============================================================================
// USER SERVICE - UserController MEJORADO con headers del Gateway
// src/main/java/com/zosh/controller/UserController.java
// =============================================================================
package com.zosh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zosh.domain.UserRole;
import com.zosh.exception.UserException;
import com.zosh.mapper.UserMapper;
import com.zosh.modal.User;
import com.zosh.payload.dto.UserDTO;
import com.zosh.payload.request.CreateUserFromCognitoRequest;
import com.zosh.repository.UserRepository;
import com.zosh.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final UserMapper userMapper;
	private final UserRepository userRepository; // 🚀 AGREGAR ESTO

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

			// 🚀 NUEVO: Verificar si es token de Cognito sin headers
			if (jwt != null && jwt.contains("cognito")) {
				System.out.println("🔍 Detectado token de Cognito sin headers del Gateway");
				return handleCognitoTokenDirect(jwt);
			}

			try {
				User user = userService.getUserFromJwtToken(jwt);
				if (user != null) {
					UserDTO userDTO = userMapper.mapToDTO(user);
					return new ResponseEntity<>(userDTO, HttpStatus.OK);
				} else {
					throw new UserException("Usuario no encontrado con JWT");
				}
			} catch (Exception e) {
				System.err.println("❌ Error con JWT tradicional (Keycloak no disponible): " + e.getMessage());

				// 🚀 FALLBACK: Si Keycloak no está disponible, intentar parsear Cognito
				if (jwt != null && (jwt.contains("cognito") || jwt.contains("amazonaws"))) {
					System.out.println("🔄 Fallback: Parseando token de Cognito directamente");
					return handleCognitoTokenDirect(jwt);
				}

				throw new UserException("Error procesando autenticación: " + e.getMessage());
			}
		}
	}

	// 🚀 NUEVO MÉTODO: Manejar token de Cognito directamente
	private ResponseEntity<UserDTO> handleCognitoTokenDirect(String jwt) {
		try {
			System.out.println("🔍 PARSEANDO TOKEN DE COGNITO DIRECTAMENTE");

			// Decodificar JWT de Cognito
			String[] parts = jwt.replace("Bearer ", "").split("\\.");
			if (parts.length < 2) {
				throw new Exception("Token JWT inválido");
			}

			String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
			System.out.println("🔍 Payload decodificado: " + payload);

			// Parsear JSON
			ObjectMapper mapper = new ObjectMapper();
			JsonNode claims = mapper.readTree(payload);

			String cognitoSub = claims.get("sub").asText();
			String email = claims.has("email") ? claims.get("email").asText() : null;
			String name = claims.has("name") ? claims.get("name").asText() : null;
			String role = claims.has("custom:role") ? claims.get("custom:role").asText() : "CUSTOMER";

			System.out.println("📋 Datos extraídos del token:");
			System.out.println("   Sub: " + cognitoSub);
			System.out.println("   Email: " + email);
			System.out.println("   Name: " + name);
			System.out.println("   Role: " + role);

			return handleCognitoUserFromGateway(cognitoSub, email, name, role);

		} catch (Exception e) {
			System.err.println("❌ Error parseando token de Cognito: " + e.getMessage());
			throw new RuntimeException("Error procesando token de Cognito: " + e.getMessage());
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

	@PostMapping("/api/users/create-from-cognito")
	public ResponseEntity<UserDTO> createUserFromCognito(@RequestBody CreateUserFromCognitoRequest request) {
		try {
			System.out.println("🚀 USER SERVICE - Creando usuario desde Cognito:");
			System.out.println("   CognitoUserId: " + request.getCognitoUserId());
			System.out.println("   Email: " + request.getEmail());
			System.out.println("   FullName: " + request.getFullName());
			System.out.println("   Role: " + request.getRole());

			User user = userService.createUserFromCognito(request);
			UserDTO userDTO = userMapper.mapToDTO(user);

			System.out.println("✅ Usuario creado exitosamente con ID: " + user.getId());
			return ResponseEntity.ok(userDTO);
		} catch (Exception e) {
			System.err.println("❌ Error creando usuario: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PutMapping("/api/users/{userId}/upgrade-to-salon-owner")
	public ResponseEntity<UserDTO> upgradeToSalonOwner(@PathVariable Long userId) {
		try {
			System.out.println("🔧 USER SERVICE - Actualizando usuario " + userId + " a SALON_OWNER");

			User user = userService.getUserById(userId);
			if (user == null) {
				throw new UserException("Usuario no encontrado con ID: " + userId);
			}

			// Actualizar el rol directamente
			user.setRole(UserRole.SALON_OWNER);
			user.setUpdatedAt(LocalDateTime.now());

			User updatedUser = userRepository.save(user);
			UserDTO userDTO = userMapper.mapToDTO(updatedUser);

			System.out.println("✅ Usuario actualizado a SALON_OWNER exitosamente");
			return ResponseEntity.ok(userDTO);
		} catch (Exception e) {
			System.err.println("❌ Error actualizando usuario: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PutMapping("/api/users/cognito/{cognitoUserId}/upgrade-to-salon-owner")
	public ResponseEntity<UserDTO> upgradeToSalonOwnerByCognitoId(@PathVariable String cognitoUserId) {
		try {
			System.out.println("🔧 USER SERVICE - Actualizando usuario " + cognitoUserId + " a SALON_OWNER");

			User user = userService.findByCognitoUserId(cognitoUserId);
			if (user == null) {
				throw new UserException("Usuario no encontrado con cognitoUserId: " + cognitoUserId);
			}

			// Actualizar el rol directamente
			user.setRole(UserRole.SALON_OWNER);
			user.setUpdatedAt(LocalDateTime.now());

			User updatedUser = userRepository.save(user);
			UserDTO userDTO = userMapper.mapToDTO(updatedUser);

			System.out.println("✅ Usuario actualizado a SALON_OWNER exitosamente");
			return ResponseEntity.ok(userDTO);
		} catch (Exception e) {
			System.err.println("❌ Error actualizando usuario: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	// 🚀 ENDPOINT 1: Buscar por Cognito ID (REQUERIDO para Gateway)
	@GetMapping("/api/users/by-cognito-id/{cognitoId}")
	public ResponseEntity<UserDTO> getUserByCognitoId(@PathVariable String cognitoId) {
		System.out.println("🔍 USER SERVICE - Buscando por Cognito ID: " + cognitoId);

		try {
			User user = userRepository.findByCognitoUserId(cognitoId);
			if (user != null) {
				UserDTO userDTO = userMapper.mapToDTO(user);
				System.out.println("✅ Usuario encontrado: " + user.getEmail() + " - Rol: " + user.getRole());
				return ResponseEntity.ok(userDTO);
			} else {
				System.out.println("❌ Usuario no encontrado con Cognito ID: " + cognitoId);
				return ResponseEntity.notFound().build();
			}
		} catch (Exception e) {
			System.err.println("❌ Error buscando usuario: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping("/api/users/by-email/{email}")
	public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
		System.out.println("🔍 USER SERVICE - Buscando por email: " + email);

		try {
			User user = userRepository.findByEmail(email);
			if (user != null) {
				UserDTO userDTO = userMapper.mapToDTO(user);
				System.out.println("✅ Usuario encontrado: " + user.getEmail() + " - Rol: " + user.getRole());
				return ResponseEntity.ok(userDTO);
			} else {
				System.out.println("❌ Usuario no encontrado con email: " + email);
				return ResponseEntity.notFound().build();
			}
		} catch (Exception e) {
			System.err.println("❌ Error buscando usuario: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

}