package com.novobanco.account.application.service;

import com.novobanco.account.application.annotation.UseCase;
import com.novobanco.account.application.port.in.ClientPort;
import com.novobanco.account.application.port.out.ClientRepositoryPort;
import com.novobanco.account.domain.enums.IdentificationType;
import com.novobanco.account.domain.exception.ClientNotFoundException;
import com.novobanco.account.domain.model.Client;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@UseCase
public class ClientService implements ClientPort {

    private final ClientRepositoryPort clientRepository;

    public ClientService(ClientRepositoryPort clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    @Transactional
    public Client createClient(String fullName, String email, String identification,
                               IdentificationType typeIdentification, String phone,
                               String address, String gender) {
        validateIdentification(identification, typeIdentification);
        if (clientRepository.existsByIdentification(identification)) {
            throw new IllegalArgumentException("Ya existe un cliente con la identificación: " + identification);
        }
        if (clientRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe un cliente con el email: " + email);
        }
        Client client = Client.builder()
                .fullName(fullName)
                .email(email)
                .identification(identification)
                .typeIdentification(typeIdentification)
                .phone(phone)
                .address(address)
                .gender(gender)
                .createdAt(LocalDateTime.now())
                .build();
        return clientRepository.save(client);
    }

    @Override
    @Transactional(readOnly = true)
    public Client getClient(String identification) {
        return clientRepository.findByIdentification(identification)
                .orElseThrow(() -> new ClientNotFoundException(identification));
    }

    private void validateIdentification(String identification, IdentificationType type) {
        if (type == IdentificationType.NATIONAL_ID || type == IdentificationType.RUC) {
            if (identification.length() > 13) {
                throw new IllegalArgumentException(type + " debe tener máximo 13 caracteres");
            }
            if (!identification.matches("\\d+")) {
                throw new IllegalArgumentException(type + " solo puede contener números");
            }
        } else if (type == IdentificationType.PASSPORT) {
            if (identification.length() > 9) {
                throw new IllegalArgumentException("El pasaporte debe tener máximo 9 caracteres");
            }
        }
    }
}
