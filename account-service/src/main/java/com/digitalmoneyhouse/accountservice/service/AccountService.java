package com.digitalmoneyhouse.accountservice.service;

import com.digitalmoneyhouse.accountservice.dto.AccountResponseDTO;
import com.digitalmoneyhouse.accountservice.dto.TransactionResponseDTO;
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

    private AccountResponseDTO toAccountResponseDTO(Account account) {
        return AccountResponseDTO.builder()
                .id(account.getId())
                .cvu(account.getCvu())
                .alias(account.getAlias())
                .userId(account.getUserId())
                .balance(account.getBalance())
                .build();
    }

    private Account findAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cuenta no encontrada con id: " + id));
    }

    public AccountResponseDTO getAccountById(Long id) {
        Account account = findAccountById(id);
        return toAccountResponseDTO(account);
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

    public List<TransactionResponseDTO> getTransactionsByAccountId(Long id) {
        findAccountById(id); // valida que la cuenta exista
        return transactionRepository
                .findByAccountIdOrderByCreatedAtDesc(id)
                .stream()
                .map(this::toTransactionResponseDTO)
                .collect(Collectors.toList());
    }
}
