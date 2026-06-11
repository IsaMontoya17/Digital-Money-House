package com.digitalmoneyhouse.usersservice.repository;

import com.digitalmoneyhouse.usersservice.model.Account;
import com.digitalmoneyhouse.usersservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByKeycloakId(String keycloakId);
    boolean existsByCvu(String cvu);
    boolean existsByAlias(String alias);
}
