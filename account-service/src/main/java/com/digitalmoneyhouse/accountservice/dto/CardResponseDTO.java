package com.digitalmoneyhouse.accountservice.dto;

import com.digitalmoneyhouse.accountservice.model.CardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CardResponseDTO {
    private Long id;
    private String cardNumber;
    private String cardHolderName;
    private String expirationDate;
    private CardType cardType;
}