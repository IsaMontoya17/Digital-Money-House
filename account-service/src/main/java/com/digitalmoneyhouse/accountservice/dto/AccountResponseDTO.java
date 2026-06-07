package com.digitalmoneyhouse.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountResponseDTO {
    private Long id;
    private String cvu;
    private String alias;
    private Long userId;
    private BigDecimal balance;
}
