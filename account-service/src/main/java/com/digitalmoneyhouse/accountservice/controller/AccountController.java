package com.digitalmoneyhouse.accountservice.controller;

import com.digitalmoneyhouse.accountservice.dto.*;
import com.digitalmoneyhouse.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
            @AuthenticationPrincipal Jwt jwt) {

        String requestingUserId = jwt.getClaimAsString("sub");
        return ResponseEntity.ok(accountService.getAccountBalance(id, requestingUserId));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponseDTO>> getTransactionsByAccountId(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        String requestingUserId = jwt.getClaimAsString("sub");
        return ResponseEntity.ok(accountService.getTransactionsByAccountId(id, requestingUserId));
    }

    @PostMapping("/{id}/cards")
    public ResponseEntity<CardResponseDTO> addCard(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CardRequestDTO request) {

        String requestingUserId = jwt.getClaimAsString("sub");
        CardResponseDTO response = accountService.addCardToAccount(id, requestingUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/cards")
    public ResponseEntity<List<CardResponseDTO>> getCards(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        String requestingUserId = jwt.getClaimAsString("sub");
        return ResponseEntity.ok(accountService.getCardsByAccountId(id, requestingUserId));
    }

    @GetMapping("/{id}/cards/{cardId}")
    public ResponseEntity<CardResponseDTO> getCardById(
            @PathVariable Long id,
            @PathVariable Long cardId,
            @AuthenticationPrincipal Jwt jwt) {

        String requestingUserId = jwt.getClaimAsString("sub");
        return ResponseEntity.ok(accountService.getCardDetail(id, cardId, requestingUserId));
    }

    @DeleteMapping("/{id}/cards/{cardId}")
    public ResponseEntity<String> deleteCard(
            @PathVariable Long id,
            @PathVariable Long cardId,
            @AuthenticationPrincipal Jwt jwt) {

        String requestingUserId = jwt.getClaimAsString("sub");
        accountService.deleteCard(id, cardId, requestingUserId);
        return ResponseEntity.ok("Tarjeta eliminada correctamente");
    }
}