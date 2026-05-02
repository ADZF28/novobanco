package com.novobanco.account.application.service;

import com.novobanco.account.application.port.out.ClientRepositoryPort;
import com.novobanco.account.domain.enums.IdentificationType;
import com.novobanco.account.domain.exception.ClientNotFoundException;
import com.novobanco.account.domain.model.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepositoryPort clientRepository;

    @InjectMocks
    private ClientService clientService;

    private Client savedClient;

    @BeforeEach
    void setUp() {
        savedClient = Client.builder()
                .id(1L).uuid(UUID.randomUUID())
                .fullName("Ana García").email("ana@test.com")
                .identification("1234567890").typeIdentification(IdentificationType.NATIONAL_ID)
                .phone("0991234567").address("Av. Test 123").gender("FEMALE")
                .createdAt(LocalDateTime.now()).build();
    }

    @Test
    void createClient_shouldReturnClient_whenDataIsValid() {
        when(clientRepository.existsByIdentification("1234567890")).thenReturn(false);
        when(clientRepository.existsByEmail("ana@test.com")).thenReturn(false);
        when(clientRepository.save(any())).thenReturn(savedClient);

        Client result = clientService.createClient(
                "Ana García", "ana@test.com", "1234567890",
                IdentificationType.NATIONAL_ID, "0991234567", "Av. Test 123", "FEMALE");

        assertThat(result.getIdentification()).isEqualTo("1234567890");
        assertThat(result.getEmail()).isEqualTo("ana@test.com");
        verify(clientRepository).save(any());
    }

    @Test
    void createClient_shouldThrow_whenIdentificationAlreadyExists() {
        when(clientRepository.existsByIdentification("1234567890")).thenReturn(true);

        assertThatThrownBy(() -> clientService.createClient(
                "Ana García", "ana@test.com", "1234567890",
                IdentificationType.NATIONAL_ID, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1234567890");

        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClient_shouldThrow_whenEmailAlreadyExists() {
        when(clientRepository.existsByIdentification(any())).thenReturn(false);
        when(clientRepository.existsByEmail("ana@test.com")).thenReturn(true);

        assertThatThrownBy(() -> clientService.createClient(
                "Ana García", "ana@test.com", "1234567890",
                IdentificationType.NATIONAL_ID, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ana@test.com");
    }

    @Test
    void createClient_shouldThrow_whenNationalIdContainsLetters() {
        assertThatThrownBy(() -> clientService.createClient(
                "Ana", "ana@test.com", "12AB567890",
                IdentificationType.NATIONAL_ID, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("números");
    }

    @Test
    void createClient_shouldThrow_whenNationalIdTooLong() {
        assertThatThrownBy(() -> clientService.createClient(
                "Ana", "ana@test.com", "12345678901234",
                IdentificationType.NATIONAL_ID, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("13");
    }

    @Test
    void createClient_shouldThrow_whenPassportTooLong() {
        assertThatThrownBy(() -> clientService.createClient(
                "John", "john@test.com", "ABCDEFGHIJ",
                IdentificationType.PASSPORT, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("9");
    }

    @Test
    void createClient_shouldAccept_validRuc() {
        when(clientRepository.existsByIdentification(any())).thenReturn(false);
        when(clientRepository.existsByEmail(any())).thenReturn(false);
        when(clientRepository.save(any())).thenReturn(savedClient);

        assertThatCode(() -> clientService.createClient(
                "Empresa", "empresa@test.com", "1792345678001",
                IdentificationType.RUC, null, null, null))
                .doesNotThrowAnyException();
    }

    @Test
    void getClient_shouldReturnClient_whenFound() {
        when(clientRepository.findByIdentification("1234567890")).thenReturn(Optional.of(savedClient));

        Client result = clientService.getClient("1234567890");

        assertThat(result.getIdentification()).isEqualTo("1234567890");
    }

    @Test
    void getClient_shouldThrow_whenNotFound() {
        when(clientRepository.findByIdentification("0000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getClient("0000000000"))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessageContaining("0000000000");
    }
}
