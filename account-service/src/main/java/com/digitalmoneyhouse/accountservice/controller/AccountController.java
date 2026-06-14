package com.digitalmoneyhouse.accountservice.controller;

import com.digitalmoneyhouse.accountservice.dto.*;
import com.digitalmoneyhouse.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<AccountResponseDTO> getAccountByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(accountService.getAccountByUserId(userId));
    }

    @GetMapping("/cvu/{cvu}")
    public ResponseEntity<AccountResponseDTO> getAccountByCvu(@PathVariable String cvu) {
        return ResponseEntity.ok(accountService.getAccountByCvu(cvu));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountBalanceDTO> getAccountById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String requestingUserId) {

        return ResponseEntity.ok(accountService.getAccountBalance(id, requestingUserId));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponseDTO>> getTransactionsByAccountId(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String requestingUserId) {

        return ResponseEntity.ok(accountService.getTransactionsByAccountId(id, requestingUserId));
    }

    @PostMapping("/{id}/cards")
    public ResponseEntity<CardResponseDTO> addCard(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String requestingUserId,
            @Valid @RequestBody CardRequestDTO request) {

        CardResponseDTO response = accountService.addCardToAccount(id, requestingUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/cards")
    public ResponseEntity<List<CardResponseDTO>> getCards(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String requestingUserId) {

        return ResponseEntity.ok(accountService.getCardsByAccountId(id, requestingUserId));
    }
}
