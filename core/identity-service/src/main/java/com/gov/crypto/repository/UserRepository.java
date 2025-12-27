package com.gov.crypto.repository;

import com.gov.crypto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    java.util.Optional<User> findByUsername(String username);

    java.util.List<User> findByIdentityStatus(User.IdentityStatus status);
}
