package com.digitalmoneyhouse.usersservice.controller;

import com.digitalmoneyhouse.usersservice.dto.RegisterRequestDTO;
import com.digitalmoneyhouse.usersservice.dto.RegisterResponseDTO;
import com.digitalmoneyhouse.usersservice.dto.UserProfileDTO;
import com.digitalmoneyhouse.usersservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO request) {
        RegisterResponseDTO response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileDTO> getUserById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        String requestingKeycloakId = jwt.getClaimAsString("sub");
        return ResponseEntity.ok(userService.getUserById(id, requestingKeycloakId));
    }
}