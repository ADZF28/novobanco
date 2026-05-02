package com.novobanco.account.application.service;

import com.novobanco.account.application.port.out.TransactionRepositoryPort;
import com.novobanco.account.domain.enums.TransactionType;
import com.novobanco.account.domain.model.Transaction;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class TransactionAuditService {

    private final TransactionRepositoryPort transactionRepository;

    public TransactionAuditService(TransactionRepositoryPort transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailed(Long accountId, TransactionType type, BigDecimal amount, String reason) {
        transactionRepository.save(Transaction.createFailed(accountId, type, amount, reason));
    }
}
