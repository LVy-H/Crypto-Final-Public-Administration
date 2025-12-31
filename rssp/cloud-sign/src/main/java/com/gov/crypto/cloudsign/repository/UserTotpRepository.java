package com.gov.crypto.cloudsign.repository;

import com.gov.crypto.cloudsign.model.UserTotp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTotpRepository extends JpaRepository<UserTotp, Long> {
    Optional<UserTotp> findByUsername(String username);
}
