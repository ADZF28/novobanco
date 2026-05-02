package com.novobanco.account.infrastructure.adapter.out.persistence;

import com.novobanco.account.application.port.out.ClientRepositoryPort;
import com.novobanco.account.domain.model.Client;
import com.novobanco.account.infrastructure.adapter.out.persistence.mapper.ClientEntityMapper;
import com.novobanco.account.infrastructure.adapter.out.persistence.repository.ClientJpaRepository;
import com.novobanco.account.infrastructure.config.annotation.PersistenceAdapter;

import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
public class ClientPersistenceAdapter implements ClientRepositoryPort {

    private final ClientJpaRepository jpaRepository;
    private final ClientEntityMapper mapper;

    public ClientPersistenceAdapter(ClientJpaRepository jpaRepository, ClientEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Client save(Client client) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(client)));
    }

    @Override
    public Optional<Client> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Client> findByUuid(UUID uuid) {
        return jpaRepository.findByUuid(uuid).map(mapper::toDomain);
    }

    @Override
    public Optional<Client> findByIdentification(String identification) {
        return jpaRepository.findByIdentification(identification).map(mapper::toDomain);
    }

    @Override
    public boolean existsByIdentification(String identification) {
        return jpaRepository.existsByIdentification(identification);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }
}
