package com.digitalmoneyhouse.accountservice.service;

import com.digitalmoneyhouse.accountservice.dto.AccountResponseDTO;
import com.digitalmoneyhouse.accountservice.exception.ResourceNotFoundException;
import com.digitalmoneyhouse.accountservice.model.Account;
import com.digitalmoneyhouse.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountResponseDTO getAccountByUserId(Long userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada para el usuario: " + userId));

        return AccountResponseDTO.builder()
                .id(account.getId())
                .cvu(account.getCvu())
                .alias(account.getAlias())
                .userId(account.getUserId())
                .build();
    }

    public AccountResponseDTO getAccountByCvu(String cvu) {
        Account account = accountRepository.findByCvu(cvu)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada con CVU: " + cvu));

        return AccountResponseDTO.builder()
                .id(account.getId())
                .cvu(account.getCvu())
                .alias(account.getAlias())
                .userId(account.getUserId())
                .build();
    }
}
