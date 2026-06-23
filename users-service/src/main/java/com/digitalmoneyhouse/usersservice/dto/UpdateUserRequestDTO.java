package com.digitalmoneyhouse.usersservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequestDTO {

    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String firstName;

    @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
    private String lastName;

    @Email(message = "El email no tiene un formato válido")
    private String email;

    @Pattern(regexp = "\\d{7,10}", message = "El DNI debe contener entre 7 y 10 dígitos")
    private String dni;

    @Pattern(regexp = "\\+?\\d{7,15}", message = "El teléfono no tiene un formato válido")
    private String phone;

    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;
}
