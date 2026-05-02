package com.novobanco.account.infrastructure.adapter.out.persistence.mapper;

import com.novobanco.account.domain.model.Transaction;
import com.novobanco.account.infrastructure.adapter.out.persistence.entity.TransactionEntity;
import org.springframework.stereotype.Component;

@Component
public class TransactionEntityMapper {

    public Transaction toDomain(TransactionEntity entity) {
        return Transaction.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .accountId(entity.getAccountId())
                .type(entity.getType())
                .amount(entity.getAmount())
                .reference(entity.getReference())
                .status(entity.getStatus())
                .description(entity.getDescription())
                .transferReference(entity.getTransferReference())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public TransactionEntity toEntity(Transaction domain) {
        TransactionEntity entity = new TransactionEntity();
        entity.setId(domain.getId());
        entity.setUuid(domain.getUuid());
        entity.setAccountId(domain.getAccountId());
        entity.setType(domain.getType());
        entity.setAmount(domain.getAmount());
        entity.setReference(domain.getReference());
        entity.setStatus(domain.getStatus());
        entity.setDescription(domain.getDescription());
        entity.setTransferReference(domain.getTransferReference());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
