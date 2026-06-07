package com.digitalmoneyhouse.accountservice.repository;

import com.digitalmoneyhouse.accountservice.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Todas las transacciones de una cuenta ordenadas de más reciente a más antigua
    List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    // Últimas N transacciones
    List<Transaction> findTop5ByAccountIdOrderByCreatedAtDesc(Long accountId);
}
