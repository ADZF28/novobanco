package com.novobanco.account.infrastructure.adapter.out.persistence;

import com.novobanco.account.application.port.out.AccountRepositoryPort;
import com.novobanco.account.domain.model.Account;
import com.novobanco.account.infrastructure.adapter.out.persistence.entity.AccountEntity;
import com.novobanco.account.infrastructure.adapter.out.persistence.mapper.AccountEntityMapper;
import com.novobanco.account.infrastructure.adapter.out.persistence.repository.AccountJpaRepository;
import com.novobanco.account.infrastructure.adapter.out.persistence.repository.ClientJpaRepository;
import com.novobanco.account.infrastructure.config.annotation.PersistenceAdapter;

import java.util.List;
import java.util.Optional;

@PersistenceAdapter
public class AccountPersistenceAdapter implements AccountRepositoryPort {

    private final AccountJpaRepository jpaRepository;
    private final ClientJpaRepository clientJpaRepository;
    private final AccountEntityMapper mapper;

    public AccountPersistenceAdapter(AccountJpaRepository jpaRepository,
                                     ClientJpaRepository clientJpaRepository,
                                     AccountEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.clientJpaRepository = clientJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Account save(Account account) {
        AccountEntity entity = mapper.toEntity(account);
        if (account.getClient() != null && account.getClient().getId() != null) {
            entity.setClient(clientJpaRepository.getReferenceById(account.getClient().getId()));
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return jpaRepository.findByAccountNumber(accountNumber).map(mapper::toDomain);
    }

    @Override
    public Optional<Account> findByAccountNumberWithLock(String accountNumber) {
        return jpaRepository.findByAccountNumberWithLock(accountNumber).map(mapper::toDomain);
    }

    @Override
    public List<Account> findByClientInternalId(Long clientId) {
        return jpaRepository.findByClient_Id(clientId).stream().map(mapper::toDomain).toList();
    }
}
