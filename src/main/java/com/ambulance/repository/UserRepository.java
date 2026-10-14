package com.ambulance.repository;

import com.ambulance.model.Role;
import com.ambulance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByRole(Role role);
}
