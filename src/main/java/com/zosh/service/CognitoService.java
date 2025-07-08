package com.zosh.service;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.JWTClaimsSet;
import com.zosh.payload.dto.CognitoUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

@Service
@RequiredArgsConstructor
public class CognitoService {

    private static final String USER_POOL_ID = "us-east-1_BM2KEBPZM";
    private static final String CLIENT_ID = "53h6m5krqkq8phqtf63ed0otu5";

    private final CognitoIdentityProviderClient cognitoClient;

    public CognitoService() {
        this.cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    /**
     * Extrae información del usuario desde un JWT de Cognito
     */
    public CognitoUserInfo extractUserInfoFromJWT(String jwtToken) throws Exception {
        try {
            // Remover "Bearer " si está presente
            if (jwtToken.startsWith("Bearer ")) {
                jwtToken = jwtToken.substring(7);
            }

            // Parsear el JWT
            JWT jwt = JWTParser.parse(jwtToken);
            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            // Extraer información del usuario
            CognitoUserInfo userInfo = new CognitoUserInfo();
            userInfo.setSub(claims.getStringClaim("sub"));
            userInfo.setEmail(claims.getStringClaim("email"));
            userInfo.setName(claims.getStringClaim("name"));
            userInfo.setPreferredUsername(claims.getStringClaim("preferred_username"));

            // Extraer rol personalizado si existe
            String customRole = claims.getStringClaim("custom:role");
            userInfo.setCustomRole(customRole != null ? customRole : "CUSTOMER");

            return userInfo;
        } catch (Exception e) {
            throw new Exception("Failed to parse Cognito JWT: " + e.getMessage());
        }
    }

    /**
     * Actualiza un atributo personalizado del usuario en Cognito
     */
    public void updateUserAttribute(String userSub, String attributeName, String attributeValue) {
        try {
            AdminUpdateUserAttributesRequest request = AdminUpdateUserAttributesRequest.builder()
                    .userPoolId(USER_POOL_ID)
                    .username(userSub)
                    .userAttributes(AttributeType.builder()
                            .name(attributeName)
                            .value(attributeValue)
                            .build())
                    .build();

            cognitoClient.adminUpdateUserAttributes(request);
        } catch (Exception e) {
            throw new RuntimeException("Error updating user attributes in Cognito: " + e.getMessage());
        }
    }

    /**
     * Obtiene información detallada del usuario desde Cognito
     */
    public AdminGetUserResponse getUserFromCognito(String userSub) {
        try {
            AdminGetUserRequest request = AdminGetUserRequest.builder()
                    .userPoolId(USER_POOL_ID)
                    .username(userSub)
                    .build();

            return cognitoClient.adminGetUser(request);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching user from Cognito: " + e.getMessage());
        }
    }
}