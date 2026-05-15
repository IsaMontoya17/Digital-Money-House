package com.digitalmoneyhouse.usersservice.repository;

import com.digitalmoneyhouse.usersservice.model.Role;
import com.digitalmoneyhouse.usersservice.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
