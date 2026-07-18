package com.digitalmoneyhouse.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRecipientDTO {
    private String cvu;
    private String alias;
    private LocalDateTime lastTransferAt;
}