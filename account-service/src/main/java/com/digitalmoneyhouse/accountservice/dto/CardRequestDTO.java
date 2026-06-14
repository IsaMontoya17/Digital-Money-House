package com.digitalmoneyhouse.accountservice.dto;

import com.digitalmoneyhouse.accountservice.model.CardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CardRequestDTO {

    @NotBlank(message = "El número de tarjeta es obligatorio")
    @Pattern(regexp = "\\d{16}", message = "El número de tarjeta debe tener 16 dígitos")
    private String cardNumber;

    @NotBlank(message = "El nombre del titular es obligatorio")
    private String cardHolderName;

    @NotBlank(message = "La fecha de expiración es obligatoria")
    @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{4}", message = "La fecha debe tener el formato MM/YYYY")
    private String expirationDate;

    @NotNull(message = "El tipo de tarjeta es obligatorio")
    private CardType cardType;
}
