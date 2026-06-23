package com.digitalmoneyhouse.accountservice.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAccountRequestDTO {

    @Size(min = 6, max = 50, message = "El alias debe tener entre 6 y 50 caracteres")
    @Pattern(regexp = "^[a-z]+\\.[a-z]+\\.[a-z]+$", message = "El alias debe tener el formato palabra.palabra.palabra")
    private String alias;
}