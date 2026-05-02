package com.novobanco.account.infrastructure.adapter.out.persistence.mapper;

import com.novobanco.account.domain.model.Client;
import com.novobanco.account.infrastructure.adapter.out.persistence.entity.ClientEntity;
import org.springframework.stereotype.Component;

@Component
public class ClientEntityMapper {

    public Client toDomain(ClientEntity entity) {
        return Client.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .identification(entity.getIdentification())
                .typeIdentification(entity.getTypeIdentification())
                .phone(entity.getPhone())
                .address(entity.getAddress())
                .gender(entity.getGender())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public ClientEntity toEntity(Client domain) {
        ClientEntity entity = new ClientEntity();
        entity.setId(domain.getId());
        entity.setUuid(domain.getUuid());
        entity.setFullName(domain.getFullName());
        entity.setEmail(domain.getEmail());
        entity.setIdentification(domain.getIdentification());
        entity.setTypeIdentification(domain.getTypeIdentification());
        entity.setPhone(domain.getPhone());
        entity.setAddress(domain.getAddress());
        entity.setGender(domain.getGender());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
