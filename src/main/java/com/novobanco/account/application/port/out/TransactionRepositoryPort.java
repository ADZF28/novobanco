package com.novobanco.account.application.port.out;

import com.novobanco.account.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepositoryPort {
    Transaction save(Transaction transaction);
    Optional<Transaction> findByReference(UUID reference);
    Page<Transaction> findByAccountId(Long accountId, Pageable pageable);
    List<Transaction> findOutgoingTransfers(List<Long> accountIds, LocalDateTime from, LocalDateTime to);
}
