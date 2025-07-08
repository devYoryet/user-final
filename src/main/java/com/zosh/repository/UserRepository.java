// =============================================================================
// USER REPOSITORY - Compatible con Oracle y tu código existente
// src/main/java/com/zosh/repository/UserRepository.java
// =============================================================================
package com.zosh.repository;

import com.zosh.modal.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	// =========================================================================
	// MÉTODO EXISTENTE - Mantener para compatibilidad
	// =========================================================================
	User findByEmail(String email);

	// =========================================================================
	// 🚀 NUEVOS MÉTODOS PARA COGNITO
	// =========================================================================

	/**
	 * Buscar usuario por su ID de Cognito
	 */
	Optional<User> findByCognitoUserId(String cognitoUserId);

	/**
	 * Buscar usuario por email (versión Optional para seguridad)
	 */
	@Query("SELECT u FROM User u WHERE u.email = :email")
	Optional<User> findByEmailOptional(@Param("email") String email);

	/**
	 * Verificar si existe un usuario con el Cognito ID dado
	 */
	@Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.cognitoUserId = :cognitoUserId")
	boolean existsByCognitoUserId(@Param("cognitoUserId") String cognitoUserId);

	/**
	 * Verificar si existe un usuario con el email dado
	 */
	boolean existsByEmail(String email);

	/**
	 * Buscar usuario por username (útil para Cognito)
	 */
	Optional<User> findByUsername(String username);

	// El método findById ya existe en JpaRepository, pero lo declaramos por
	// claridad
	Optional<User> findById(Long id);
}

// =============================================================================
// NOTA: Si quieres hacer el método findByEmail más seguro sin romper código:
// =============================================================================
/*
 * Puedes crear una versión híbrida en UserServiceImpl:
 * 
 * @Override
 * public User findByEmail(String email) throws UserException {
 * // Usar la versión segura internamente
 * return userRepository.findByEmailOptional(email)
 * .orElseThrow(() -> new UserException("Usuario no encontrado con email: " +
 * email));
 * }
 * 
 * Y mantener el método original para compatibilidad con código existente.
 */