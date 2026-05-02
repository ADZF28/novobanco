package com.novobanco.account.infrastructure.adapter.out.persistence.repository;

import com.novobanco.account.domain.enums.TransactionType;
import com.novobanco.account.infrastructure.adapter.out.persistence.entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, Long> {
    Optional<TransactionEntity> findByReference(UUID reference);
    Page<TransactionEntity> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);
    List<TransactionEntity> findByAccountIdInAndTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
            List<Long> accountIds, TransactionType type, LocalDateTime from, LocalDateTime to);
}
