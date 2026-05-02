package com.novobanco.account.infrastructure.adapter.out.persistence;

import com.novobanco.account.domain.enums.AccountStatus;
import com.novobanco.account.domain.enums.AccountType;
import com.novobanco.account.domain.enums.IdentificationType;
import com.novobanco.account.domain.model.Account;
import com.novobanco.account.domain.model.Client;
import com.novobanco.account.infrastructure.adapter.out.persistence.entity.AccountEntity;
import com.novobanco.account.infrastructure.adapter.out.persistence.entity.ClientEntity;
import com.novobanco.account.infrastructure.adapter.out.persistence.mapper.AccountEntityMapper;
import com.novobanco.account.infrastructure.adapter.out.persistence.mapper.ClientEntityMapper;
import com.novobanco.account.infrastructure.adapter.out.persistence.repository.AccountJpaRepository;
import com.novobanco.account.infrastructure.adapter.out.persistence.repository.ClientJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountPersistenceAdapterTest {

    @Mock
    private AccountJpaRepository accountJpaRepository;

    @Mock
    private ClientJpaRepository clientJpaRepository;

    private AccountPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        ClientEntityMapper clientMapper = new ClientEntityMapper();
        AccountEntityMapper accountMapper = new AccountEntityMapper(clientMapper);
        adapter = new AccountPersistenceAdapter(accountJpaRepository, clientJpaRepository, accountMapper);
    }

    private ClientEntity buildClientEntity() {
        ClientEntity e = new ClientEntity();
        e.setId(1L);
        e.setUuid(UUID.randomUUID());
        e.setFullName("Ana García");
        e.setEmail("ana@test.com");
        e.setIdentification("1234567890");
        e.setTypeIdentification(IdentificationType.NATIONAL_ID);
        e.setCreatedAt(LocalDateTime.now());
        return e;
    }

    private AccountEntity buildAccountEntity(String accountNumber) {
        AccountEntity e = new AccountEntity();
        e.setId(1L);
        e.setUuid(UUID.randomUUID());
        e.setAccountNumber(accountNumber);
        e.setClient(buildClientEntity());
        e.setType(AccountType.SAVINGS);
        e.setCurrency("USD");
        e.setBalance(new BigDecimal("1000.00"));
        e.setStatus(AccountStatus.ACTIVE);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }

    private Account buildAccount(String accountNumber) {
        Client client = Client.builder()
                .id(1L).uuid(UUID.randomUUID()).fullName("Ana García")
                .email("ana@test.com").identification("1234567890")
                .typeIdentification(IdentificationType.NATIONAL_ID)
                .createdAt(LocalDateTime.now()).build();
        return Account.builder()
                .id(1L).uuid(UUID.randomUUID()).accountNumber(accountNumber).client(client)
                .type(AccountType.SAVINGS).currency("USD")
                .balance(new BigDecimal("1000.00")).status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    @Test
    void save_shouldPersistAndReturnMappedAccount() {
        AccountEntity entity = buildAccountEntity("NB-001");
        when(clientJpaRepository.getReferenceById(1L)).thenReturn(buildClientEntity());
        when(accountJpaRepository.save(any())).thenReturn(entity);

        Account result = adapter.save(buildAccount("NB-001"));

        assertThat(result.getAccountNumber()).isEqualTo("NB-001");
        assertThat(result.getBalance()).isEqualByComparingTo("1000.00");
        verify(accountJpaRepository).save(any());
    }

    @Test
    void findByAccountNumber_shouldReturnAccount_whenExists() {
        when(accountJpaRepository.findByAccountNumber("NB-001"))
                .thenReturn(Optional.of(buildAccountEntity("NB-001")));

        Optional<Account> result = adapter.findByAccountNumber("NB-001");

        assertThat(result).isPresent();
        assertThat(result.get().getAccountNumber()).isEqualTo("NB-001");
    }

    @Test
    void findByAccountNumber_shouldReturnEmpty_whenNotExists() {
        when(accountJpaRepository.findByAccountNumber("NB-999")).thenReturn(Optional.empty());

        Optional<Account> result = adapter.findByAccountNumber("NB-999");

        assertThat(result).isEmpty();
    }

    @Test
    void findByAccountNumberWithLock_shouldReturnAccount_whenExists() {
        when(accountJpaRepository.findByAccountNumberWithLock("NB-001"))
                .thenReturn(Optional.of(buildAccountEntity("NB-001")));

        Optional<Account> result = adapter.findByAccountNumberWithLock("NB-001");

        assertThat(result).isPresent();
        verify(accountJpaRepository).findByAccountNumberWithLock("NB-001");
    }

    @Test
    void findByClientInternalId_shouldReturnListOfAccounts() {
        List<AccountEntity> entities = List.of(
                buildAccountEntity("NB-001"),
                buildAccountEntity("NB-002"));
        when(accountJpaRepository.findByClient_Id(1L)).thenReturn(entities);

        List<Account> result = adapter.findByClientInternalId(1L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Account::getAccountNumber)
                .containsExactlyInAnyOrder("NB-001", "NB-002");
    }

    @Test
    void findByClientInternalId_shouldReturnEmptyList_whenNoAccounts() {
        when(accountJpaRepository.findByClient_Id(99L)).thenReturn(List.of());

        List<Account> result = adapter.findByClientInternalId(99L);

        assertThat(result).isEmpty();
    }
}
