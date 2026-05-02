package com.novobanco.account.infrastructure.adapter.out.persistence;

import com.novobanco.account.domain.enums.IdentificationType;
import com.novobanco.account.domain.model.Client;
import com.novobanco.account.infrastructure.adapter.out.persistence.entity.ClientEntity;
import com.novobanco.account.infrastructure.adapter.out.persistence.mapper.ClientEntityMapper;
import com.novobanco.account.infrastructure.adapter.out.persistence.repository.ClientJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientPersistenceAdapterTest {

    @Mock
    private ClientJpaRepository jpaRepository;

    private ClientPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ClientPersistenceAdapter(jpaRepository, new ClientEntityMapper());
    }

    private ClientEntity buildEntity() {
        ClientEntity e = new ClientEntity();
        e.setId(1L);
        e.setUuid(UUID.randomUUID());
        e.setFullName("Ana García");
        e.setEmail("ana@test.com");
        e.setIdentification("1234567890");
        e.setTypeIdentification(IdentificationType.NATIONAL_ID);
        e.setPhone("0991234567");
        e.setAddress("Av. Test 123");
        e.setGender("FEMALE");
        e.setCreatedAt(LocalDateTime.now());
        return e;
    }

    private Client buildDomain() {
        return Client.builder()
                .id(1L).uuid(UUID.randomUUID())
                .fullName("Ana García").email("ana@test.com")
                .identification("1234567890").typeIdentification(IdentificationType.NATIONAL_ID)
                .phone("0991234567").address("Av. Test 123").gender("FEMALE")
                .createdAt(LocalDateTime.now()).build();
    }

    @Test
    void save_shouldPersistAndReturnMappedClient() {
        when(jpaRepository.save(any())).thenReturn(buildEntity());

        Client result = adapter.save(buildDomain());

        assertThat(result.getIdentification()).isEqualTo("1234567890");
        assertThat(result.getEmail()).isEqualTo("ana@test.com");
        verify(jpaRepository).save(any());
    }

    @Test
    void findByIdentification_shouldReturnClient_whenExists() {
        when(jpaRepository.findByIdentification("1234567890")).thenReturn(Optional.of(buildEntity()));

        Optional<Client> result = adapter.findByIdentification("1234567890");

        assertThat(result).isPresent();
        assertThat(result.get().getIdentification()).isEqualTo("1234567890");
        assertThat(result.get().getFullName()).isEqualTo("Ana García");
    }

    @Test
    void findByIdentification_shouldReturnEmpty_whenNotExists() {
        when(jpaRepository.findByIdentification("0000000000")).thenReturn(Optional.empty());

        Optional<Client> result = adapter.findByIdentification("0000000000");

        assertThat(result).isEmpty();
    }

    @Test
    void findById_shouldReturnClient_whenExists() {
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(buildEntity()));

        Optional<Client> result = adapter.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findByUuid_shouldReturnClient_whenExists() {
        UUID uuid = UUID.randomUUID();
        ClientEntity entity = buildEntity();
        entity.setUuid(uuid);
        when(jpaRepository.findByUuid(uuid)).thenReturn(Optional.of(entity));

        Optional<Client> result = adapter.findByUuid(uuid);

        assertThat(result).isPresent();
        assertThat(result.get().getUuid()).isEqualTo(uuid);
    }

    @Test
    void existsByIdentification_shouldReturnTrue_whenExists() {
        when(jpaRepository.existsByIdentification("1234567890")).thenReturn(true);

        assertThat(adapter.existsByIdentification("1234567890")).isTrue();
    }

    @Test
    void existsByIdentification_shouldReturnFalse_whenNotExists() {
        when(jpaRepository.existsByIdentification("0000000000")).thenReturn(false);

        assertThat(adapter.existsByIdentification("0000000000")).isFalse();
    }

    @Test
    void existsByEmail_shouldReturnTrue_whenExists() {
        when(jpaRepository.existsByEmail("ana@test.com")).thenReturn(true);

        assertThat(adapter.existsByEmail("ana@test.com")).isTrue();
    }

    @Test
    void existsByEmail_shouldReturnFalse_whenNotExists() {
        when(jpaRepository.existsByEmail("nobody@test.com")).thenReturn(false);

        assertThat(adapter.existsByEmail("nobody@test.com")).isFalse();
    }
}
