package com.digitalmoneyhouse.accountservice.service;

import com.digitalmoneyhouse.accountservice.dto.*;
import com.digitalmoneyhouse.accountservice.exception.*;
import com.digitalmoneyhouse.accountservice.model.Account;
import com.digitalmoneyhouse.accountservice.model.Card;
import com.digitalmoneyhouse.accountservice.model.Transaction;
import com.digitalmoneyhouse.accountservice.model.TransactionType;
import com.digitalmoneyhouse.accountservice.repository.AccountRepository;
import com.digitalmoneyhouse.accountservice.repository.CardRepository;
import com.digitalmoneyhouse.accountservice.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final Logger transactionLog = LoggerFactory.getLogger("AUDIT_TRANSACTIONS");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;

    private static final int MAX_RECENT_RECIPIENTS = 5;

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

    public CardResponseDTO addCardToAccount(Long accountId, String requestingUserId, CardRequestDTO request) {
        Account account = findAccountById(accountId);

        if (!account.getUserId().equals(requestingUserId)) {
            throw new ForbiddenException("No tienes permisos para modificar esta cuenta");
        }

        if (cardRepository.findByCardNumber(request.getCardNumber()).isPresent()) {
            throw new ConflictException("La tarjeta ya está asociada a una cuenta");
        }

        Card card = Card.builder()
                .cardNumber(request.getCardNumber())
                .cardHolderName(request.getCardHolderName())
                .expirationDate(request.getExpirationDate())
                .cardType(request.getCardType())
                .account(account)
                .build();

        Card savedCard = cardRepository.save(card);

        return CardResponseDTO.builder()
                .id(savedCard.getId())
                .cardNumber(savedCard.getCardNumber())
                .cardHolderName(savedCard.getCardHolderName())
                .expirationDate(savedCard.getExpirationDate())
                .cardType(savedCard.getCardType())
                .build();
    }

    public List<CardResponseDTO> getCardsByAccountId(Long accountId, String requestingUserId) {
        Account account = findAccountById(accountId);

        if (!account.getUserId().equals(requestingUserId)) {
            throw new ForbiddenException("No tienes permisos para acceder a esta cuenta");
        }

        return cardRepository.findByAccountId(accountId)
                .stream()
                .map(card -> CardResponseDTO.builder()
                        .id(card.getId())
                        .cardNumber(card.getCardNumber())
                        .cardHolderName(card.getCardHolderName())
                        .expirationDate(card.getExpirationDate())
                        .cardType(card.getCardType())
                        .build())
                .collect(Collectors.toList());
    }

    public CardResponseDTO getCardDetail(Long accountId, Long cardId, String requestingUserId) {
        Account account = findAccountById(accountId);

        if (!account.getUserId().equals(requestingUserId)) {
            throw new ForbiddenException("No tienes permisos para acceder a esta cuenta");
        }

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarjeta no encontrada con id: " + cardId));

        if (!card.getAccount().getId().equals(accountId)) {
            throw new ResourceNotFoundException("Tarjeta no encontrada con id: " + cardId);
        }

        return CardResponseDTO.builder()
                .id(card.getId())
                .cardNumber(card.getCardNumber())
                .cardHolderName(card.getCardHolderName())
                .expirationDate(card.getExpirationDate())
                .cardType(card.getCardType())
                .build();
    }

    public void deleteCard(Long accountId, Long cardId, String requestingUserId) {
        Account account = findAccountById(accountId);

        if (!account.getUserId().equals(requestingUserId)) {
            throw new ForbiddenException("No tienes permisos para acceder a esta cuenta");
        }

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarjeta no encontrada con id: " + cardId));

        if (!card.getAccount().getId().equals(accountId)) {
            throw new ResourceNotFoundException("Tarjeta no encontrada con id: " + cardId);
        }

        cardRepository.delete(card);
    }

    public AccountResponseDTO updateAccount(Long id, UpdateAccountRequestDTO request, String requestingUserId) {
        Account account = findAccountById(id);

        if (!account.getUserId().equals(requestingUserId)) {
            throw new ForbiddenException("No tienes permisos para modificar esta cuenta");
        }

        if (request.getAlias() != null) {
            if (accountRepository.existsByAlias(request.getAlias()) && !request.getAlias().equals(account.getAlias())) {
                throw new ConflictException("El alias ya está en uso");
            }
            account.setAlias(request.getAlias());
        }

        accountRepository.save(account);
        return toAccountResponseDTO(account);
    }

    public List<TransactionResponseDTO> getActivityByAccountId(Long id, String requestingUserId) {
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

    public TransactionResponseDTO getActivityDetail(Long accountId, Long transactionId, String requestingUserId) {
        Account account = findAccountById(accountId);

        if (!account.getUserId().equals(requestingUserId)) {
            throw new ForbiddenException("No tienes permisos para acceder a esta cuenta");
        }

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transacción no encontrada con id: " + transactionId));

        if (!transaction.getAccount().getId().equals(accountId)) {
            throw new ResourceNotFoundException(
                    "Transacción no encontrada con id: " + transactionId);
        }

        return toTransactionResponseDTO(transaction);
    }

    public TransactionResponseDTO deposit(Long accountId, TransferenceRequestDTO request, String requestingUserId) {
        Account account = findAccountById(accountId);

        if (!account.getUserId().equals(requestingUserId)) {
            throw new ForbiddenException("No tienes permisos para operar en esta cuenta");
        }

        Card card = cardRepository.findById(request.getCardId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tarjeta no encontrada con id: " + request.getCardId()));

        if (!card.getAccount().getId().equals(accountId)) {
            throw new ForbiddenException("La tarjeta no pertenece a esta cuenta");
        }

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        String description = request.getDescription() != null
                ? request.getDescription()
                : "Depósito desde tarjeta terminada en " +
                card.getCardNumber().substring(card.getCardNumber().length() - 4);

        Transaction transaction = Transaction.builder()
                .account(account)
                .amount(request.getAmount())
                .type(TransactionType.INCOME)
                .description(description)
                .build();

        Transaction saved = transactionRepository.save(transaction);

        transactionLog.info("DEPOSIT accountId={} amount={} cardId={} transactionId={}",
                accountId, request.getAmount(), request.getCardId(), saved.getId());

        return toTransactionResponseDTO(saved);
    }

    public List<TransferRecipientDTO> getLastRecipients(Long id, String requestingUserId) {
        Account account = findAccountById(id);

        if (!account.getUserId().equals(requestingUserId)) {
            throw new ForbiddenException("No tienes permisos para acceder a esta cuenta");
        }

        List<Transaction> outgoingTransfers = transactionRepository
                .findByAccountIdAndTypeOrderByCreatedAtDesc(id, TransactionType.TRANSFER_OUT);

        Map<String, Transaction> latestByRecipient = new LinkedHashMap<>();
        for (Transaction t : outgoingTransfers) {
            latestByRecipient.putIfAbsent(t.getDestCvu(), t);
        }

        return latestByRecipient.values().stream()
                .limit(MAX_RECENT_RECIPIENTS)
                .map(t -> {
                    String alias = accountRepository.findByCvu(t.getDestCvu())
                            .map(Account::getAlias)
                            .orElse(null);

                    return TransferRecipientDTO.builder()
                            .cvu(t.getDestCvu())
                            .alias(alias)
                            .lastTransferAt(t.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionResponseDTO transfer(Long id, TransferRequestDTO request, String requestingUserId) {
        Account origin = findAccountById(id);

        if (!origin.getUserId().equals(requestingUserId)) {
            throw new ForbiddenException("No tienes permisos para operar en esta cuenta");
        }

        Account destination = accountRepository.findByCvu(request.getDestination())
                .or(() -> accountRepository.findByAlias(request.getDestination()))
                .orElseThrow(() -> new ValidationException("La cuenta destino no existe"));

        if (destination.getId().equals(origin.getId())) {
            throw new ValidationException("No puedes transferirte dinero a vos mismo");
        }

        if (origin.getBalance().compareTo(request.getAmount()) < 0) {
            transactionLog.warn("TRANSFER_REJECTED reason=insufficient_funds accountId={} requestedAmount={} availableBalance={}",
                    origin.getId(), request.getAmount(), origin.getBalance());
            throw new InsufficientFundsException("Fondos insuficientes para realizar la transferencia");
        }

        origin.setBalance(origin.getBalance().subtract(request.getAmount()));
        destination.setBalance(destination.getBalance().add(request.getAmount()));
        accountRepository.save(origin);
        accountRepository.save(destination);

        String description = request.getDescription() != null
                ? request.getDescription()
                : "Transferencia a " + destination.getAlias();

        Transaction outTransaction = Transaction.builder()
                .account(origin)
                .amount(request.getAmount())
                .type(TransactionType.TRANSFER_OUT)
                .description(description)
                .originCvu(origin.getCvu())
                .destCvu(destination.getCvu())
                .build();
        transactionRepository.save(outTransaction);

        Transaction inTransaction = Transaction.builder()
                .account(destination)
                .amount(request.getAmount())
                .type(TransactionType.TRANSFER_IN)
                .description("Transferencia recibida de " + origin.getAlias())
                .originCvu(origin.getCvu())
                .destCvu(destination.getCvu())
                .build();
        transactionRepository.save(inTransaction);

        transactionLog.info("TRANSFER originAccountId={} destAccountId={} amount={} outTransactionId={} inTransactionId={}",
                origin.getId(), destination.getId(), request.getAmount(), outTransaction.getId(), inTransaction.getId());

        return toTransactionResponseDTO(outTransaction);
    }
}