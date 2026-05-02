package com.novobanco.account.infrastructure.adapter.out.persistence;

import com.novobanco.account.domain.enums.TransactionStatus;
import com.novobanco.account.domain.enums.TransactionType;
import com.novobanco.account.domain.model.Transaction;
import com.novobanco.account.infrastructure.adapter.out.persistence.entity.TransactionEntity;
import com.novobanco.account.infrastructure.adapter.out.persistence.mapper.TransactionEntityMapper;
import com.novobanco.account.infrastructure.adapter.out.persistence.repository.TransactionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionPersistenceAdapterTest {

    @Mock
    private TransactionJpaRepository jpaRepository;

    private TransactionPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TransactionPersistenceAdapter(jpaRepository, new TransactionEntityMapper());
    }

    private TransactionEntity buildEntity(TransactionType type) {
        TransactionEntity e = new TransactionEntity();
        e.setId(1L);
        e.setUuid(UUID.randomUUID());
        e.setAccountId(1L);
        e.setType(type);
        e.setAmount(new BigDecimal("100.00"));
        e.setReference(UUID.randomUUID());
        e.setStatus(TransactionStatus.SUCCESS);
        e.setDescription("test");
        e.setCreatedAt(LocalDateTime.now());
        return e;
    }

    private Transaction buildDomain(TransactionType type) {
        return Transaction.builder()
                .id(1L).uuid(UUID.randomUUID()).accountId(1L).type(type)
                .amount(new BigDecimal("100.00")).reference(UUID.randomUUID())
                .status(TransactionStatus.SUCCESS).description("test")
                .createdAt(LocalDateTime.now()).build();
    }

    @Test
    void save_shouldPersistAndReturnMappedTransaction() {
        TransactionEntity entity = buildEntity(TransactionType.DEPOSIT);
        when(jpaRepository.save(any())).thenReturn(entity);

        Transaction result = adapter.save(buildDomain(TransactionType.DEPOSIT));

        assertThat(result.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(result.getAmount()).isEqualByComparingTo("100.00");
        verify(jpaRepository).save(any());
    }

    @Test
    void findByReference_shouldReturnTransaction_whenExists() {
        UUID ref = UUID.randomUUID();
        TransactionEntity entity = buildEntity(TransactionType.DEPOSIT);
        entity.setReference(ref);
        when(jpaRepository.findByReference(ref)).thenReturn(Optional.of(entity));

        Optional<Transaction> result = adapter.findByReference(ref);

        assertThat(result).isPresent();
        assertThat(result.get().getType()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    void findByReference_shouldReturnEmpty_whenNotExists() {
        UUID ref = UUID.randomUUID();
        when(jpaRepository.findByReference(ref)).thenReturn(Optional.empty());

        Optional<Transaction> result = adapter.findByReference(ref);

        assertThat(result).isEmpty();
    }

    @Test
    void findByAccountId_shouldReturnPagedTransactions() {
        TransactionEntity entity = buildEntity(TransactionType.WITHDRAWAL);
        PageRequest pageable = PageRequest.of(0, 20);
        Page<TransactionEntity> page = new PageImpl<>(List.of(entity), pageable, 1);
        when(jpaRepository.findByAccountIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(page);

        Page<Transaction> result = adapter.findByAccountId(1L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(TransactionType.WITHDRAWAL);
    }

    @Test
    void findOutgoingTransfers_shouldReturnOnlyTransferDebits() {
        TransactionEntity debit1 = buildEntity(TransactionType.TRANSFER_DEBIT);
        TransactionEntity debit2 = buildEntity(TransactionType.TRANSFER_DEBIT);
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to = LocalDateTime.now();

        when(jpaRepository.findByAccountIdInAndTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
                List.of(1L, 2L), TransactionType.TRANSFER_DEBIT, from, to))
                .thenReturn(List.of(debit1, debit2));

        List<Transaction> result = adapter.findOutgoingTransfers(List.of(1L, 2L), from, to);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> t.getType() == TransactionType.TRANSFER_DEBIT);
    }

    @Test
    void findOutgoingTransfers_shouldReturnEmpty_whenNoneInRange() {
        when(jpaRepository.findByAccountIdInAndTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
                any(), any(), any(), any()))
                .thenReturn(List.of());

        List<Transaction> result = adapter.findOutgoingTransfers(
                List.of(1L), LocalDateTime.now().minusDays(7), LocalDateTime.now());

        assertThat(result).isEmpty();
    }
}
