package com.wltogether.repository;

import com.wltogether.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmailOrUsername(String email, String username);
    Optional<User> findByUid(String uid);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByUid(String uid);
    boolean existsByRole(String role);
    Page<User> findByStatus(String status, Pageable pageable);
}
