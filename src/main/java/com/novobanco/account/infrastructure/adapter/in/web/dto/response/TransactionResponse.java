package com.novobanco.account.infrastructure.adapter.in.web.dto.response;

import com.novobanco.account.domain.enums.TransactionStatus;
import com.novobanco.account.domain.enums.TransactionType;
import com.novobanco.account.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID uuid,
        TransactionType type,
        BigDecimal amount,
        UUID reference,
        TransactionStatus status,
        String description,
        UUID transferReference,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getUuid(),
                tx.getType(),
                tx.getAmount(),
                tx.getReference(),
                tx.getStatus(),
                tx.getDescription(),
                tx.getTransferReference(),
                tx.getCreatedAt()
        );
    }
}
