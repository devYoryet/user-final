package com.zosh.modal;

import com.zosh.domain.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "USERS") // mismo nombre que tu script
@SequenceGenerator( // ← enlaza con USERS_SEQ
        name = "users_seq", sequenceName = "USERS_SEQ", // nombre real de la secuencia
        allocationSize = 1) // 1 = NEXTVAL cada insert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq") // usa la secuencia
    private Long id;

    @NotBlank(message = "full name is mandatory")
    @Column(name = "FULL_NAME", nullable = false)
    private String fullName;

    @NotBlank(message = "username is mandatory")
    @Column(name = "USERNAME", nullable = false, unique = true, length = 100)
    private String username;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    @Column(name = "EMAIL", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "PHONE", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 50)
    private UserRole role = UserRole.CUSTOMER;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;
}
