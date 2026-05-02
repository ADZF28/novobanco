package com.novobanco.account.infrastructure.adapter.in.web.dto.response;

import com.novobanco.account.domain.enums.IdentificationType;
import com.novobanco.account.domain.model.Client;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClientResponse(
        UUID uuid,
        String fullName,
        String email,
        String identification,
        IdentificationType typeIdentification,
        String phone,
        String address,
        String gender,
        LocalDateTime createdAt
) {
    public static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getUuid(),
                client.getFullName(),
                client.getEmail(),
                client.getIdentification(),
                client.getTypeIdentification(),
                client.getPhone(),
                client.getAddress(),
                client.getGender(),
                client.getCreatedAt()
        );
    }
}
