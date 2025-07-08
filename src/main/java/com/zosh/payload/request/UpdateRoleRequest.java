
package com.zosh.payload.request;

import lombok.Data;

@Data
public class UpdateRoleRequest {
    private String role; // CUSTOMER, SALON_OWNER, ADMIN
}