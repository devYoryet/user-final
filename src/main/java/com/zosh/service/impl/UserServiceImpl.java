// =============================================================================
// USER SERVICE IMPL - Corregido para Oracle y sin password
// src/main/java/com/zosh/service/impl/UserServiceImpl.java
// =============================================================================
package com.zosh.service.impl;

import com.zosh.domain.UserRole;
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
		System.out.println("🔍 USER SERVICE - Buscando usuario por cognitoUserId: " + cognitoUserId);

		User user = userRepository.findByCognitoUserId(cognitoUserId);
		if (user == null) {
			System.out.println("❌ Usuario no encontrado con cognitoUserId: " + cognitoUserId);
			throw new UserException("Usuario no encontrado con Cognito ID: " + cognitoUserId);
		}

		System.out.println("✅ Usuario encontrado: " + user.getEmail() + " (ID: " + user.getId() + ")");
		return user;
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
		System.out.println("🚀 USER SERVICE - Creando usuario desde Cognito:");
		System.out.println("   Cognito ID: " + request.getCognitoUserId());
		System.out.println("   Email: " + request.getEmail());
		System.out.println("   Nombre: " + request.getFullName());
		System.out.println("   Rol: " + request.getRole());

		// Verificar si ya existe un usuario con este cognitoUserId
		User existingUser = userRepository.findByCognitoUserId(request.getCognitoUserId());
		if (existingUser != null) {
			System.out.println("⚠️ Usuario ya existe con cognitoUserId: " + request.getCognitoUserId());
			return existingUser;
		}

		// Verificar si ya existe un usuario con este email
		User existingUserByEmail = userRepository.findByEmail(request.getEmail());
		if (existingUserByEmail != null) {
			System.out.println("⚠️ Usuario ya existe con email: " + request.getEmail());
			// Actualizar el cognitoUserId del usuario existente
			existingUserByEmail.setCognitoUserId(request.getCognitoUserId());
			existingUserByEmail.setUpdatedAt(LocalDateTime.now());
			return userRepository.save(existingUserByEmail);
		}

		// Crear nuevo usuario
		User newUser = new User();
		newUser.setEmail(request.getEmail());
		newUser.setFullName(request.getFullName() != null ? request.getFullName() : request.getEmail());
		newUser.setUsername(request.getEmail()); // Usar email como username
		newUser.setCognitoUserId(request.getCognitoUserId());
		newUser.setRole(parseUserRole(request.getRole()));
		newUser.setPhone(""); // Campo vacío por defecto
		newUser.setCreatedAt(LocalDateTime.now());
		newUser.setUpdatedAt(LocalDateTime.now());

		User savedUser = userRepository.save(newUser);
		System.out.println("✅ Usuario creado exitosamente con ID: " + savedUser.getId());

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
	// Método helper para parsear roles
	private UserRole parseUserRole(String roleString) {
		try {
			if (roleString != null) {
				// Remover prefijo ROLE_ si existe y convertir a uppercase
				String cleanRole = roleString.replace("ROLE_", "").toUpperCase();
				return UserRole.valueOf(cleanRole);
			} else {
				return UserRole.CUSTOMER;
			}
		} catch (IllegalArgumentException e) {
			System.err.println("⚠️ Rol no válido: " + roleString + ", usando CUSTOMER por defecto");
			return UserRole.CUSTOMER;
		}
	}

	@Override
	public User upgradeToSalonOwner(Long userId) throws UserException {
		User user = getUserById(userId);
		if (user == null) {
			throw new UserException("Usuario no encontrado");
		}

		// Actualizar el rol
		user.setRole(UserRole.SALON_OWNER);

		System.out.println("🔧 Actualizando usuario " + userId + " a SALON_OWNER");
		return userRepository.save(user);
	}

	@Override
	public User upgradeToSalonOwnerByCognitoId(String cognitoUserId) throws UserException {
		User user = findByCognitoUserId(cognitoUserId);
		if (user == null) {
			throw new UserException("Usuario no encontrado con cognitoUserId: " + cognitoUserId);
		}

		// Actualizar el rol
		user.setRole(UserRole.SALON_OWNER);

		System.out.println("🔧 Actualizando usuario " + cognitoUserId + " a SALON_OWNER");
		return userRepository.save(user);
	}

	@Override
	public boolean hasExistingSalon(Long userId) {
		// Aquí deberías inyectar SalonService o SalonRepository
		// return salonRepository.existsByOwnerId(userId);
		return false; // Placeholder - implementar según tu lógica
	}

	@Override
	public boolean hasExistingSalonByCognitoId(String cognitoUserId) {
		try {
			User user = findByCognitoUserId(cognitoUserId);
			return user != null && hasExistingSalon(user.getId());
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Actualiza el rol del usuario a SALON_OWNER
	 * Usa el repositorio directamente ya que no tienes updateUser
	 */
	public User upgradeUserToSalonOwner(Long userId) throws UserException {
		User user = getUserById(userId);
		if (user == null) {
			throw new UserException("Usuario no encontrado con ID: " + userId);
		}

		System.out.println("🔧 Actualizando usuario " + userId + " a SALON_OWNER");

		// Actualizar el rol
		user.setRole(UserRole.SALON_OWNER);
		user.setUpdatedAt(LocalDateTime.now());

		User updatedUser = userRepository.save(user);
		System.out.println("✅ Usuario actualizado exitosamente a SALON_OWNER");

		return updatedUser;
	}

	/**
	 * Actualiza el rol del usuario a SALON_OWNER por cognitoUserId
	 */
	public User upgradeUserToSalonOwnerByCognitoId(String cognitoUserId) throws UserException {
		User user = findByCognitoUserId(cognitoUserId);
		if (user == null) {
			throw new UserException("Usuario no encontrado con cognitoUserId: " + cognitoUserId);
		}

		System.out.println("🔧 Actualizando usuario " + cognitoUserId + " a SALON_OWNER");

		// Actualizar el rol
		user.setRole(UserRole.SALON_OWNER);
		user.setUpdatedAt(LocalDateTime.now());

		User updatedUser = userRepository.save(user);
		System.out.println("✅ Usuario actualizado exitosamente a SALON_OWNER");

		return updatedUser;
	}
}