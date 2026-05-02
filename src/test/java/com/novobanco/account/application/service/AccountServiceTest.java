package com.novobanco.account.application.service;

import com.novobanco.account.application.port.out.AccountRepositoryPort;
import com.novobanco.account.application.port.out.ClientRepositoryPort;
import com.novobanco.account.domain.enums.AccountStatus;
import com.novobanco.account.domain.enums.AccountType;
import com.novobanco.account.domain.enums.IdentificationType;
import com.novobanco.account.domain.exception.ClientNotFoundException;
import com.novobanco.account.domain.exception.InsufficientInitialDepositException;
import com.novobanco.account.domain.model.Account;
import com.novobanco.account.domain.model.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepositoryPort accountRepository;

    @Mock
    private ClientRepositoryPort clientRepository;

    @InjectMocks
    private AccountService accountService;

    private Client existingClient;
    private static final String IDENTIFICATION = "0987654321";

    @BeforeEach
    void setUp() {
        existingClient = Client.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .fullName("Juan Pérez")
                .email("juan@test.com")
                .identification(IDENTIFICATION)
                .typeIdentification(IdentificationType.NATIONAL_ID)
                .phone("0991234567")
                .address("Av. Principal 123")
                .gender("MALE")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createAccount_savings_shouldAllowZeroInitialBalance() {
        when(clientRepository.findByIdentification(IDENTIFICATION)).thenReturn(Optional.of(existingClient));
        when(accountRepository.save(any())).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            return Account.builder()
                    .id(1L)
                    .uuid(UUID.randomUUID())
                    .accountNumber(a.getAccountNumber())
                    .client(a.getClient())
                    .type(a.getType())
                    .currency(a.getCurrency())
                    .balance(a.getBalance())
                    .status(a.getStatus())
                    .createdAt(a.getCreatedAt())
                    .updatedAt(a.getUpdatedAt())
                    .build();
        });

        Account result = accountService.createAccount(IDENTIFICATION, AccountType.SAVINGS, AccountStatus.ACTIVE, BigDecimal.ZERO);

        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(result.getType()).isEqualTo(AccountType.SAVINGS);
        assertThat(result.getAccountNumber()).hasSize(15);
    }

    @Test
    void createAccount_savings_shouldAllowNullInitialBalanceAsZero() {
        when(clientRepository.findByIdentification(IDENTIFICATION)).thenReturn(Optional.of(existingClient));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.createAccount(IDENTIFICATION, AccountType.SAVINGS, AccountStatus.ACTIVE, null);

        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void createAccount_checking_shouldSucceed_whenInitialBalanceAtLeast50() {
        when(clientRepository.findByIdentification(IDENTIFICATION)).thenReturn(Optional.of(existingClient));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.createAccount(IDENTIFICATION, AccountType.CHECKING, AccountStatus.ACTIVE, new BigDecimal("50.00"));

        assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.getType()).isEqualTo(AccountType.CHECKING);
    }

    @Test
    void createAccount_checking_shouldThrow_whenInitialBalanceBelowMinimum() {
        when(clientRepository.findByIdentification(IDENTIFICATION)).thenReturn(Optional.of(existingClient));

        assertThatThrownBy(() ->
                accountService.createAccount(IDENTIFICATION, AccountType.CHECKING, AccountStatus.ACTIVE, new BigDecimal("49.99")))
                .isInstanceOf(InsufficientInitialDepositException.class)
                .hasMessageContaining("50");
    }

    @Test
    void createAccount_checking_shouldThrow_whenNoInitialBalance() {
        when(clientRepository.findByIdentification(IDENTIFICATION)).thenReturn(Optional.of(existingClient));

        assertThatThrownBy(() ->
                accountService.createAccount(IDENTIFICATION, AccountType.CHECKING, AccountStatus.ACTIVE, null))
                .isInstanceOf(InsufficientInitialDepositException.class);
    }

    @Test
    void createAccount_shouldGenerateUniqueAccountNumbers() {
        when(clientRepository.findByIdentification(IDENTIFICATION)).thenReturn(Optional.of(existingClient));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        accountService.createAccount(IDENTIFICATION, AccountType.SAVINGS, AccountStatus.ACTIVE, BigDecimal.ZERO);
        accountService.createAccount(IDENTIFICATION, AccountType.SAVINGS, AccountStatus.BLOCKED, BigDecimal.ZERO);

        verify(accountRepository, times(2)).save(captor.capture());
        var numbers = captor.getAllValues().stream().map(Account::getAccountNumber).toList();
        assertThat(numbers.get(0)).isNotEqualTo(numbers.get(1));
    }

    @Test
    void createAccount_shouldThrowClientNotFoundException_whenClientNotFound() {
        when(clientRepository.findByIdentification(IDENTIFICATION)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                accountService.createAccount(IDENTIFICATION, AccountType.SAVINGS, AccountStatus.ACTIVE, BigDecimal.ZERO))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessageContaining(IDENTIFICATION);
    }

    @Test
    void createAccount_shouldSetInitialStatusFromRequest() {
        when(clientRepository.findByIdentification(IDENTIFICATION)).thenReturn(Optional.of(existingClient));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.createAccount(IDENTIFICATION, AccountType.SAVINGS, AccountStatus.BLOCKED, BigDecimal.ZERO);

        assertThat(result.getStatus()).isEqualTo(AccountStatus.BLOCKED);
    }
}
