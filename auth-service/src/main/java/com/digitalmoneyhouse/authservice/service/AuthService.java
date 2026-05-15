package com.digitalmoneyhouse.authservice.service;

import com.digitalmoneyhouse.authservice.dto.LoginRequestDTO;
import com.digitalmoneyhouse.authservice.dto.LoginResponseDTO;
import com.digitalmoneyhouse.authservice.exception.ResourceNotFoundException;
import com.digitalmoneyhouse.authservice.model.User;
import com.digitalmoneyhouse.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.security.oauth2.client.provider.keycloak.token-uri}")
    private String keycloakTokenUri;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}")
    private String keycloakIssuerUri;

    public LoginResponseDTO login(LoginRequestDTO request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Contraseña incorrecta");
        }

        return getKeycloakToken(request.getEmail(), request.getPassword());
    }

    private LoginResponseDTO getKeycloakToken(String email, String password) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", email);
        body.add("password", password);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                keycloakTokenUri, entity, Map.class
        );

        Map<String, Object> responseBody = response.getBody();

        return LoginResponseDTO.builder()
                .accessToken((String) responseBody.get("access_token"))
                .tokenType((String) responseBody.get("token_type"))
                .expiresIn(((Number) responseBody.get("expires_in")).longValue())
                .build();
    }

    public void logout(String token) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", token);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        // Extraer el refresh_token no es posible solo con el access_token,
        // así que invalidamos la sesión via el endpoint de logout de Keycloak
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            String logoutUrl = keycloakIssuerUri + "/protocol/openid-connect/logout";
            restTemplate.postForEntity(logoutUrl, entity, String.class);
        } catch (Exception e) {
            // Si falla el logout en Keycloak, igual respondemos OK
            // El token expirará naturalmente en 5 minutos
        }
    }
}
