package com.novobanco.account.infrastructure.adapter.out.persistence.repository;

import com.novobanco.account.infrastructure.adapter.out.persistence.entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientJpaRepository extends JpaRepository<ClientEntity, Long> {
    boolean existsByIdentification(String identification);
    boolean existsByEmail(String email);
    Optional<ClientEntity> findByUuid(UUID uuid);
    Optional<ClientEntity> findByIdentification(String identification);
}
