package com.zosh.payload.request;

public class UpdateCognitoIdRequest {
    private String cognitoUserId;

    public String getCognitoUserId() {
        return cognitoUserId;
    }

    public void setCognitoUserId(String cognitoUserId) {
        this.cognitoUserId = cognitoUserId;
    }
}