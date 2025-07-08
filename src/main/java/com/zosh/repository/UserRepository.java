// =============================================================================
// USER SERVICE - UserRepository con método findByCognitoUserId
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

	// Métodos existentes
	User findByEmail(String email);

	// 🚀 MÉTODOS PARA COGNITO

	/**
	 * Busca usuario por cognitoUserId
	 */
	User findByCognitoUserId(String cognitoUserId);

	/**
	 * Verifica si existe un usuario con cognitoUserId
	 */
	boolean existsByCognitoUserId(String cognitoUserId);

	/**
	 * Busca usuario por email ignorando mayúsculas/minúsculas
	 */
	@Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
	User findByEmailIgnoreCase(@Param("email") String email);

	/**
	 * Busca usuario por cognitoUserId o email
	 */
	@Query("SELECT u FROM User u WHERE u.cognitoUserId = :cognitoUserId OR LOWER(u.email) = LOWER(:email)")
	User findByCognitoUserIdOrEmail(@Param("cognitoUserId") String cognitoUserId, @Param("email") String email);

}