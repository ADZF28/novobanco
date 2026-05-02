package com.novobanco.account.application.port.out;

import com.novobanco.account.domain.model.Client;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepositoryPort {
    Client save(Client client);
    Optional<Client> findById(Long id);
    Optional<Client> findByUuid(UUID uuid);
    Optional<Client> findByIdentification(String identification);
    boolean existsByIdentification(String identification);
    boolean existsByEmail(String email);
}
