// =============================================================================
// USER SERVICE IMPL - Corregido para Oracle y sin password
// src/main/java/com/zosh/service/impl/UserServiceImpl.java
// =============================================================================
package com.zosh.service.impl;

import com.zosh.exception.UserException;
import com.zosh.modal.User;
import com.zosh.payload.dto.KeycloakUserinfo;
import com.zosh.payload.request.CreateUserFromCognitoRequest;
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

	// =========================================================================
	// MÉTODOS EXISTENTES
	// =========================================================================

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
		KeycloakUserinfo userinfo = keycloakUserService.fetchUserProfileByJwt(jwt);
		return userRepository.findByEmail(userinfo.getEmail());
	}

	@Override
	public User getUserById(Long id) throws UserException {
		return userRepository.findById(id)
				.orElseThrow(() -> new UserException("User not found with id: " + id));
	}

	@Override
	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

	// =========================================================================
	// 🚀 NUEVOS MÉTODOS PARA COGNITO
	// =========================================================================

	@Override
	public User findByCognitoUserId(String cognitoUserId) throws UserException {
		return userRepository.findByCognitoUserId(cognitoUserId)
				.orElseThrow(() -> new UserException("Usuario no encontrado con Cognito ID: " + cognitoUserId));
	}

	@Override
	public User findByEmail(String email) throws UserException {
		User user = userRepository.findByEmail(email);
		if (user == null) {
			throw new UserException("Usuario no encontrado con email: " + email);
		}
		return user;
	}

	@Override
	public User createUserFromCognito(CreateUserFromCognitoRequest request) {
		System.out.println("🔧 Creando usuario desde Cognito:");
		System.out.println("   Cognito ID: " + request.getCognitoUserId());
		System.out.println("   Email: " + request.getEmail());
		System.out.println("   Nombre: " + request.getFullName());
		System.out.println("   Rol: " + request.getRole());

		// ✅ USANDO BUILDER PATTERN (compatible con tu modelo)
		User user = User.builder()
				.cognitoUserId(request.getCognitoUserId())
				.email(request.getEmail())
				.fullName(request.getFullName() != null ? request.getFullName() : request.getEmail())
				.username(request.getEmail()) // Usar email como username
				.phone("") // Campo vacío por defecto
				.role(parseUserRole(request.getRole()))
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		User savedUser = userRepository.save(user);
		System.out.println("✅ Usuario creado con ID: " + savedUser.getId());

		return savedUser;
	}

	@Override
	public void updateCognitoUserId(Long userId, String cognitoUserId) throws UserException {
		User user = getUserById(userId);
		user.setCognitoUserId(cognitoUserId);
		user.setUpdatedAt(LocalDateTime.now());
		userRepository.save(user);

		System.out.println("✅ CognitoUserId actualizado para usuario " + userId + ": " + cognitoUserId);
	}

	// =========================================================================
	// MÉTODO HELPER PARA PARSEAR ROLES
	// =========================================================================

	private com.zosh.domain.UserRole parseUserRole(String roleString) {
		try {
			if (roleString != null) {
				// Remover prefijo ROLE_ si existe y convertir a uppercase
				String cleanRole = roleString.replace("ROLE_", "").toUpperCase();
				return com.zosh.domain.UserRole.valueOf(cleanRole);
			} else {
				return com.zosh.domain.UserRole.CUSTOMER;
			}
		} catch (IllegalArgumentException e) {
			System.err.println("⚠️ Rol no válido: " + roleString + ", usando CUSTOMER por defecto");
			return com.zosh.domain.UserRole.CUSTOMER;
		}
	}
}