package com.novobanco.account.infrastructure.adapter.out.persistence;

import com.novobanco.account.application.port.out.TransactionRepositoryPort;
import com.novobanco.account.domain.enums.TransactionType;
import com.novobanco.account.domain.model.Transaction;
import com.novobanco.account.infrastructure.adapter.out.persistence.mapper.TransactionEntityMapper;
import com.novobanco.account.infrastructure.adapter.out.persistence.repository.TransactionJpaRepository;
import com.novobanco.account.infrastructure.config.annotation.PersistenceAdapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
public class TransactionPersistenceAdapter implements TransactionRepositoryPort {

    private final TransactionJpaRepository jpaRepository;
    private final TransactionEntityMapper mapper;

    public TransactionPersistenceAdapter(TransactionJpaRepository jpaRepository,
                                         TransactionEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Transaction save(Transaction transaction) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(transaction)));
    }

    @Override
    public Optional<Transaction> findByReference(UUID reference) {
        return jpaRepository.findByReference(reference).map(mapper::toDomain);
    }

    @Override
    public Page<Transaction> findByAccountId(Long accountId, Pageable pageable) {
        return jpaRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public List<Transaction> findOutgoingTransfers(List<Long> accountIds, LocalDateTime from, LocalDateTime to) {
        return jpaRepository
                .findByAccountIdInAndTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
                        accountIds, TransactionType.TRANSFER_DEBIT, from, to)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
