package com.gov.crypto.cloudsign.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_totp_secrets")
public class UserTotp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String secretKey;
    
    // Default constructor for JPA
    public UserTotp() {}

    public UserTotp(String username, String secretKey) {
        this.username = username;
        this.secretKey = secretKey;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}
