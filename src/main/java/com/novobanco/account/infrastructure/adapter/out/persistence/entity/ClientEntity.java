package com.novobanco.account.infrastructure.adapter.out.persistence.entity;

import com.novobanco.account.domain.enums.IdentificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
public class ClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(nullable = false, length = 200, unique = true)
    private String email;

    @Column(nullable = false, length = 50, unique = true)
    private String identification;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_identification", nullable = false, length = 20)
    private IdentificationType typeIdentification;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String address;

    @Column(length = 30)
    private String gender;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onPersist() {
        if (uuid == null) uuid = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
