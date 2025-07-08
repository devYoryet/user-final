// Actualizar src/main/java/com/zosh/controller/UserController.java
package com.zosh.controller;

import com.zosh.exception.UserException;
import com.zosh.mapper.UserMapper;
import com.zosh.modal.User;
import com.zosh.payload.dto.UserDTO;
import com.zosh.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final UserMapper userMapper;

	@GetMapping("/api/users/profile")
	public ResponseEntity<?> getUserFromJwtToken(
			@RequestHeader("Authorization") String jwt) {
		try {
			System.out.println("=== GET USER PROFILE ===");
			System.out.println("Authorization header: " + (jwt != null ? "Present" : "Missing"));

			if (jwt == null || jwt.trim().isEmpty()) {
				return ResponseEntity.badRequest().body("Authorization header is missing");
			}

			User user = userService.getUserFromJwtToken(jwt);

			if (user == null) {
				return ResponseEntity.notFound().build();
			}

			UserDTO userDTO = userMapper.mapToDTO(user);
			System.out.println("Usuario encontrado: " + userDTO.getEmail() + " - Role: " + userDTO.getRole());

			return ResponseEntity.ok(userDTO);

		} catch (Exception e) {
			System.out.println("Error en getUserFromJwtToken: " + e.getMessage());
			e.printStackTrace();

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body("Invalid or expired token: " + e.getMessage());
		}
	}

	@GetMapping("/api/users/{userId}")
	public ResponseEntity<UserDTO> getUserById(@PathVariable Long userId) throws UserException {
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

	// Endpoint para testing
	@GetMapping("/api/users/test")
	public ResponseEntity<?> testEndpoint() {
		return ResponseEntity.ok("USER Service is working!");
	}
}