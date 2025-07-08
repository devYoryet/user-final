package com.zosh.payload.dto;

import lombok.Data;

@Data
public class CognitoUserInfo {
    private String sub;
    private String email;
    private String name;
    private String preferredUsername;
    private String customRole;
    private String phoneNumber;
}