package com.digitalmoneyhouse.accountservice.service;

import com.digitalmoneyhouse.accountservice.dto.AccountBalanceDTO;
import com.digitalmoneyhouse.accountservice.dto.AccountResponseDTO;
import com.digitalmoneyhouse.accountservice.dto.TransactionResponseDTO;
import com.digitalmoneyhouse.accountservice.exception.ForbiddenException;
import com.digitalmoneyhouse.accountservice.exception.ResourceNotFoundException;
import com.digitalmoneyhouse.accountservice.model.Account;
import com.digitalmoneyhouse.accountservice.model.Transaction;
import com.digitalmoneyhouse.accountservice.repository.AccountRepository;
import com.digitalmoneyhouse.accountservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    private Account findAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cuenta no encontrada con id: " + id));
    }

    private AccountResponseDTO toAccountResponseDTO(Account account) {
        return AccountResponseDTO.builder()
                .id(account.getId())
                .cvu(account.getCvu())
                .alias(account.getAlias())
                .userId(account.getUserId())
                .balance(account.getBalance())
                .build();
    }

    private TransactionResponseDTO toTransactionResponseDTO(Transaction t) {
        return TransactionResponseDTO.builder()
                .id(t.getId())
                .amount(t.getAmount())
                .type(t.getType())
                .createdAt(t.getCreatedAt())
                .description(t.getDescription())
                .originCvu(t.getOriginCvu())
                .destCvu(t.getDestCvu())
                .build();
    }

    public AccountResponseDTO getAccountByUserId(String userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cuenta no encontrada para el usuario: " + userId));
        return toAccountResponseDTO(account);
    }

    public AccountResponseDTO getAccountByCvu(String cvu) {
        Account account = accountRepository.findByCvu(cvu)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cuenta no encontrada con CVU: " + cvu));
        return toAccountResponseDTO(account);
    }

    public AccountBalanceDTO getAccountBalance(Long id, String requestingUserId) {
        Account account = findAccountById(id);

        if (!account.getUserId().equals(requestingUserId)) {
            throw new ForbiddenException("No tienes permisos para acceder a esta cuenta");
        }

        return AccountBalanceDTO.builder()
                .id(account.getId())
                .balance(account.getBalance())
                .build();
    }

    public List<TransactionResponseDTO> getTransactionsByAccountId(Long id, String requestingUserId) {
        Account account = findAccountById(id);

        if (!account.getUserId().equals(requestingUserId)) {
            throw new ForbiddenException("No tienes permisos para acceder a esta cuenta");
        }

        return transactionRepository
                .findByAccountIdOrderByCreatedAtDesc(id)
                .stream()
                .map(this::toTransactionResponseDTO)
                .collect(Collectors.toList());
    }
}