// src/main/java/com/zosh/domain/UserRole.java
package com.zosh.domain;

public enum UserRole {
    CUSTOMER,
    ADMIN,
    SALON_OWNER;

    // Método para normalizar roles desde diferentes fuentes
    public static UserRole fromString(String role) {
        if (role == null || role.isEmpty()) {
            return CUSTOMER;
        }

        // Limpiar el string
        String cleanRole = role.toUpperCase()
                .replace("ROLE_", "")
                .replace("COGNITO_", "")
                .trim();

        switch (cleanRole) {
            case "SALON_OWNER":
            case "SALONOWNER":
                return SALON_OWNER;
            case "ADMIN":
            case "ADMINISTRATOR":
                return ADMIN;
            case "CUSTOMER":
            case "USER":
            default:
                return CUSTOMER;
        }
    }

    // Método para obtener el string del rol
    public String getRoleString() {
        return this.name();
    }
}