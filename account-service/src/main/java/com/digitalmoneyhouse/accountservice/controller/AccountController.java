package com.digitalmoneyhouse.accountservice.controller;

import com.digitalmoneyhouse.accountservice.dto.AccountResponseDTO;
import com.digitalmoneyhouse.accountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<AccountResponseDTO> getAccountByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(accountService.getAccountByUserId(userId));
    }

    @GetMapping("/cvu/{cvu}")
    public ResponseEntity<AccountResponseDTO> getAccountByCvu(@PathVariable String cvu) {
        return ResponseEntity.ok(accountService.getAccountByCvu(cvu));
    }
}
