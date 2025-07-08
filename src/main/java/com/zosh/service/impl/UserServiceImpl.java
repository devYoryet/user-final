// Actualizar UserServiceImpl.java en el microservicio USER
package com.zosh.service.impl;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.zosh.domain.UserRole;
import com.zosh.exception.UserException;
import com.zosh.modal.User;
import com.zosh.payload.dto.KeycloakUserinfo;
import com.zosh.repository.UserRepository;
import com.zosh.service.KeycloakUserService;
import com.zosh.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final KeycloakUserService keycloakUserService;

	@Override
	public User getUserByEmail(String email) throws UserException {
		User user = userRepository.findByEmail(email);
		if (user == null) {
			throw new UserException("User not found with email: " + email);
		}
		return user;
	}

	@Override
	public User getUserFromJwtToken(String jwt) throws Exception {
		System.out.println("=== VALIDANDO JWT EN USER SERVICE ===");
		System.out.println("JWT recibido: " + (jwt != null ? "SÍ" : "NO"));

		if (jwt == null || jwt.trim().isEmpty()) {
			throw new Exception("JWT token is null or empty");
		}

		// Remover "Bearer " si está presente
		if (jwt.startsWith("Bearer ")) {
			jwt = jwt.substring(7);
		}

		System.out
				.println("JWT procesado (primeros 50 chars): " + jwt.substring(0, Math.min(50, jwt.length())) + "...");

		try {
			// Intentar primero como JWT de Cognito
			return getUserFromCognitoJWT(jwt);
		} catch (Exception cognitoException) {
			System.out.println("Fallo validación Cognito: " + cognitoException.getMessage());

			try {
				// Si falla, intentar con Keycloak
				KeycloakUserinfo userinfo = keycloakUserService.fetchUserProfileByJwt("Bearer " + jwt);
				return userRepository.findByEmail(userinfo.getEmail());
			} catch (Exception keycloakException) {
				System.out.println("Fallo validación Keycloak: " + keycloakException.getMessage());
				throw new Exception("Failed to validate JWT from both Cognito and Keycloak. " +
						"Cognito: " + cognitoException.getMessage() +
						", Keycloak: " + keycloakException.getMessage());
			}
		}
	}

	/**
	 * Procesa JWT de Cognito y obtiene/crea usuario
	 */
	private User getUserFromCognitoJWT(String jwt) throws Exception {
		try {
			System.out.println("=== PROCESANDO JWT COGNITO ===");

			// Parsear el JWT de Cognito (sin validar firma por ahora)
			JWT jwtParsed = JWTParser.parse(jwt);
			JWTClaimsSet claims = jwtParsed.getJWTClaimsSet();

			System.out.println("JWT parseado exitosamente");
			System.out.println("Issuer: " + claims.getIssuer());
			System.out.println("Subject: " + claims.getSubject());
			System.out.println("Email: " + claims.getStringClaim("email"));
			System.out.println("Username: " + claims.getStringClaim("username"));

			// Verificar que es un JWT de Cognito
			String issuer = claims.getIssuer();
			if (issuer == null || !issuer.contains("cognito-idp.us-east-1.amazonaws.com")) {
				throw new Exception("Not a valid Cognito JWT. Issuer: " + issuer);
			}

			// Extraer información del usuario
			String email = claims.getStringClaim("email");
			String username = claims.getStringClaim("username");
			String sub = claims.getSubject();

			if (email == null) {
				// Si no hay email en el token, intentar derivarlo del username o sub
				email = username != null ? username + "@cognito.local" : sub + "@cognito.local";
				System.out.println("Email no encontrado en JWT, usando: " + email);
			}

			System.out.println("Buscando usuario en BD con email: " + email);

			// Buscar o crear usuario en la base de datos
			User user = userRepository.findByEmail(email);

			if (user == null) {
				System.out.println("Usuario no existe, creando nuevo usuario");

				// Crear nuevo usuario
				user = new User();
				user.setEmail(email);
				user.setUsername(username != null ? username : email.split("@")[0]);
				user.setFullName(email); // Por defecto usar email como nombre
				user.setRole(UserRole.CUSTOMER); // Rol por defecto
				user.setCreatedAt(LocalDateTime.now());
				user.setUpdatedAt(LocalDateTime.now());

				user = userRepository.save(user);
				System.out.println("Usuario creado con ID: " + user.getId());
			} else {
				System.out.println("Usuario encontrado: " + user.getId() + " - " + user.getEmail());
			}

			return user;

		} catch (Exception e) {
			System.out.println("Error procesando JWT de Cognito: " + e.getMessage());
			e.printStackTrace();
			throw new Exception("Failed to process Cognito JWT: " + e.getMessage());
		}
	}

	@Override
	public User getUserById(Long id) throws UserException {
		return userRepository.findById(id).orElse(null);
	}

	@Override
	public List<User> getAllUsers() {
		return userRepository.findAll();
	}
}