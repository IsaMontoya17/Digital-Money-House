package com.digitalmoneyhouse.usersservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class KeycloakService {

    @Value("${keycloak.admin.server-url}")
    private String serverUrl;

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.client-secret}")
    private String clientSecret;

    public void createUser(String email, String password, String firstName, String lastName) {
        String adminToken = getAdminToken();

        String userJson = String.format("""
            {
                "username": "%s",
                "email": "%s",
                "firstName": "%s",
                "lastName": "%s",
                "enabled": true,
                "emailVerified": true,
                "credentials": [{
                    "type": "password",
                    "value": "%s",
                    "temporary": false
                }]
            }
            """, email, email, firstName, lastName, password);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        HttpEntity<String> entity = new HttpEntity<>(userJson, headers);
        String url = serverUrl + "/admin/realms/" + realm + "/users";

        restTemplate.postForEntity(url, entity, String.class);
    }

    private String getAdminToken() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
        String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);
        return (String) response.getBody().get("access_token");
    }
}
