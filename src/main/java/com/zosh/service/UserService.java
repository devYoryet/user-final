// =============================================================================
// USER SERVICE - Interfaz actualizada
// src/main/java/com/zosh/service/UserService.java
// =============================================================================
package com.zosh.service;

import com.zosh.exception.UserException;
import com.zosh.modal.User;
import com.zosh.payload.request.CreateUserFromCognitoRequest;

import java.util.List;

public interface UserService {

	// Métodos existentes
	User getUserByEmail(String email) throws UserException;

	User getUserFromJwtToken(String jwt) throws Exception;

	User getUserById(Long id) throws UserException;

	List<User> getAllUsers();

	// 🚀 NUEVOS MÉTODOS PARA COGNITO
	User findByCognitoUserId(String cognitoUserId) throws UserException;

	User findByEmail(String email) throws UserException;

	User createUserFromCognito(CreateUserFromCognitoRequest request);

	void updateCognitoUserId(Long userId, String cognitoUserId) throws UserException;
}