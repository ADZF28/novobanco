package com.novobanco.account.infrastructure.adapter.out.persistence.mapper;

import com.novobanco.account.domain.model.Account;
import com.novobanco.account.infrastructure.adapter.out.persistence.entity.AccountEntity;
import org.springframework.stereotype.Component;

@Component
public class AccountEntityMapper {

    private final ClientEntityMapper clientMapper;

    public AccountEntityMapper(ClientEntityMapper clientMapper) {
        this.clientMapper = clientMapper;
    }

    public Account toDomain(AccountEntity entity) {
        return Account.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .accountNumber(entity.getAccountNumber())
                .client(clientMapper.toDomain(entity.getClient()))
                .type(entity.getType())
                .currency(entity.getCurrency())
                .balance(entity.getBalance())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public AccountEntity toEntity(Account domain) {
        AccountEntity entity = new AccountEntity();
        entity.setId(domain.getId());
        entity.setUuid(domain.getUuid());
        entity.setAccountNumber(domain.getAccountNumber());
        entity.setType(domain.getType());
        entity.setCurrency(domain.getCurrency());
        entity.setBalance(domain.getBalance());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
