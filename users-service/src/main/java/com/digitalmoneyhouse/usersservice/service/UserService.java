package com.digitalmoneyhouse.usersservice.service;

import com.digitalmoneyhouse.usersservice.dto.RegisterRequestDTO;
import com.digitalmoneyhouse.usersservice.dto.RegisterResponseDTO;
import com.digitalmoneyhouse.usersservice.dto.UpdateUserRequestDTO;
import com.digitalmoneyhouse.usersservice.dto.UserProfileDTO;
import com.digitalmoneyhouse.usersservice.exception.ForbiddenException;
import com.digitalmoneyhouse.usersservice.exception.ResourceNotFoundException;
import com.digitalmoneyhouse.usersservice.model.Account;
import com.digitalmoneyhouse.usersservice.model.Role;
import com.digitalmoneyhouse.usersservice.model.RoleName;
import com.digitalmoneyhouse.usersservice.model.User;
import com.digitalmoneyhouse.usersservice.repository.AccountRepository;
import com.digitalmoneyhouse.usersservice.repository.RoleRepository;
import com.digitalmoneyhouse.usersservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakService keycloakService;

    public RegisterResponseDTO registerUser(RegisterRequestDTO request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        if (userRepository.existsByDni(request.getDni())) {
            throw new IllegalArgumentException("El DNI ya está registrado");
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.save(
                        Role.builder().name(RoleName.ROLE_USER).build()
                ));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .dni(request.getDni())
                .phone(request.getPhone())
                .roles(new HashSet<>(Set.of(userRole)))
                .build();

        User savedUser = userRepository.save(user);

        String keycloakId = keycloakService.createUser(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName()
        );

        savedUser.setKeycloakId(keycloakId);
        userRepository.save(savedUser);

        Account account = Account.builder()
                .cvu(generateUniqueCvu())
                .alias(generateUniqueAlias())
                .keycloakId(keycloakId)
                .build();

        Account savedAccount = accountRepository.save(account);

        return RegisterResponseDTO.builder()
                .id(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .dni(savedUser.getDni())
                .phone(savedUser.getPhone())
                .cvu(savedAccount.getCvu())
                .alias(savedAccount.getAlias())
                .build();
    }

    private String generateUniqueCvu() {
        String cvu;
        do {
            cvu = generateCvu();
        } while (accountRepository.existsByCvu(cvu));
        return cvu;
    }

    private String generateCvu() {
        StringBuilder cvu = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 22; i++) {
            cvu.append(random.nextInt(10));
        }
        return cvu.toString();
    }

    private String generateUniqueAlias() {
        String alias;
        do {
            alias = generateAlias();
        } while (accountRepository.existsByAlias(alias));
        return alias;
    }

    private String generateAlias() {
        String[] words = {
                "perro", "gato", "casa", "auto", "luna", "sol", "mar", "rio",
                "monte", "campo", "flor", "viento", "fuego", "agua", "tierra",
                "cielo", "nube", "estrella", "piedra", "bosque"
        };
        Random random = new Random();
        return words[random.nextInt(words.length)] + "." +
                words[random.nextInt(words.length)] + "." +
                words[random.nextInt(words.length)];
    }

    public UserProfileDTO getUserById(Long id, String requestingKeycloakId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        if (!user.getKeycloakId().equals(requestingKeycloakId)) {
            throw new ForbiddenException("No tienes permisos para ver este perfil");
        }

        Account account = accountRepository.findByKeycloakId(user.getKeycloakId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada para el usuario: " + id));

        return UserProfileDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .dni(user.getDni())
                .phone(user.getPhone())
                .cvu(account.getCvu())
                .alias(account.getAlias())
                .build();
    }

    public UserProfileDTO updateUser(Long id, UpdateUserRequestDTO request, String requestingKeycloakId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        if (!user.getKeycloakId().equals(requestingKeycloakId)) {
            throw new ForbiddenException("No tienes permisos para modificar este perfil");
        }

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            if (userRepository.existsByEmail(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
                throw new IllegalArgumentException("El email ya está registrado");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getDni() != null) {
            if (userRepository.existsByDni(request.getDni()) && !request.getDni().equals(user.getDni())) {
                throw new IllegalArgumentException("El DNI ya está registrado");
            }
            user.setDni(request.getDni());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            keycloakService.updateUserPassword(user.getKeycloakId(), request.getPassword());
        }

        userRepository.save(user);

        Account account = accountRepository.findByKeycloakId(user.getKeycloakId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada para el usuario: " + id));

        return UserProfileDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .dni(user.getDni())
                .phone(user.getPhone())
                .cvu(account.getCvu())
                .alias(account.getAlias())
                .build();
    }
}