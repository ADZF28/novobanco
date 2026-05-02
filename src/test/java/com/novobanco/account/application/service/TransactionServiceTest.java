package com.novobanco.account.application.service;

import com.novobanco.account.application.port.out.AccountRepositoryPort;
import com.novobanco.account.application.port.out.ClientRepositoryPort;
import com.novobanco.account.application.port.out.TransactionRepositoryPort;
import com.novobanco.account.domain.enums.AccountStatus;
import com.novobanco.account.domain.enums.AccountType;
import com.novobanco.account.domain.enums.TransactionType;
import com.novobanco.account.domain.exception.AccountNotOperableException;
import com.novobanco.account.domain.exception.DuplicateTransactionException;
import com.novobanco.account.domain.exception.InsufficientFundsException;
import com.novobanco.account.domain.model.Account;
import com.novobanco.account.domain.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
class TransactionServiceTest {

    @Mock
    private AccountRepositoryPort accountRepository;

    @Mock
    private TransactionRepositoryPort transactionRepository;

    @Mock
    private ClientRepositoryPort clientRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account activeAccount;
    private Account blockedAccount;

    @BeforeEach
    void setUp() {
        activeAccount = Account.builder()
                .id(1L).uuid(UUID.randomUUID()).accountNumber("NB001")
                .type(AccountType.SAVINGS).currency("USD")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        blockedAccount = Account.builder()
                .id(2L).uuid(UUID.randomUUID()).accountNumber("NB002")
                .type(AccountType.SAVINGS).currency("USD")
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.BLOCKED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void deposit_shouldCreditAccount_whenAccountIsActive() {
        when(transactionRepository.findByReference(any())).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("NB001")).thenReturn(Optional.of(activeAccount));
        when(accountRepository.save(any())).thenReturn(activeAccount);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        transactionService.deposit("NB001", new BigDecimal("200.00"), null, "test");

        assertThat(activeAccount.getBalance()).isEqualByComparingTo("1200.00");
    }

    @Test
    void deposit_shouldThrowAccountNotOperable_whenAccountIsBlocked() {
        when(transactionRepository.findByReference(any())).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("NB002")).thenReturn(Optional.of(blockedAccount));

        assertThatThrownBy(() ->
                transactionService.deposit("NB002", new BigDecimal("100.00"), null, null))
                .isInstanceOf(AccountNotOperableException.class)
                .hasMessageContaining("bloqueada");
    }

    @Test
    void deposit_shouldThrowDuplicateTransaction_whenReferenceAlreadyExists() {
        UUID ref = UUID.randomUUID();
        Transaction existing = Transaction.createDeposit(activeAccount.getId(),
                new BigDecimal("100"), ref, "dup");
        when(transactionRepository.findByReference(ref)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
                transactionService.deposit("NB001", new BigDecimal("100"), ref, null))
                .isInstanceOf(DuplicateTransactionException.class);
    }

    @Test
    void withdraw_shouldDebitAccount_whenSufficientFunds() {
        when(transactionRepository.findByReference(any())).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumberWithLock("NB001")).thenReturn(Optional.of(activeAccount));
        when(accountRepository.save(any())).thenReturn(activeAccount);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        transactionService.withdraw("NB001", new BigDecimal("300.00"), null, null);

        assertThat(activeAccount.getBalance()).isEqualByComparingTo("700.00");
    }

    @Test
    void withdraw_shouldThrowInsufficientFunds_whenBalanceIsNotEnough() {
        when(transactionRepository.findByReference(any())).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumberWithLock("NB001")).thenReturn(Optional.of(activeAccount));

        assertThatThrownBy(() ->
                transactionService.withdraw("NB001", new BigDecimal("2000.00"), null, null))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("NB001");
    }

    @Test
    void withdraw_shouldNotAllowNegativeBalance() {
        when(transactionRepository.findByReference(any())).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumberWithLock("NB001")).thenReturn(Optional.of(activeAccount));

        assertThatThrownBy(() ->
                transactionService.withdraw("NB001", new BigDecimal("1000.01"), null, null))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(activeAccount.getBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void transfer_shouldMoveMoneyAtomically() {
        Account destination = Account.builder()
                .id(2L).uuid(UUID.randomUUID()).accountNumber("NB003")
                .type(AccountType.CHECKING).currency("USD")
                .balance(new BigDecimal("200.00"))
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(transactionRepository.findByReference(any())).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumberWithLock("NB001")).thenReturn(Optional.of(activeAccount));
        when(accountRepository.findByAccountNumberWithLock("NB003")).thenReturn(Optional.of(destination));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Transaction> result = transactionService.transfer(
                "NB001", "NB003", new BigDecimal("300.00"), null, "pago");

        assertThat(activeAccount.getBalance()).isEqualByComparingTo("700.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("500.00");
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getType()).isEqualTo(TransactionType.TRANSFER_DEBIT);
        assertThat(result.get(1).getType()).isEqualTo(TransactionType.TRANSFER_CREDIT);
        assertThat(result.get(0).getTransferReference()).isEqualTo(result.get(1).getTransferReference());
    }

    @Test
    void transfer_shouldRejectSelfTransfer() {
        assertThatThrownBy(() ->
                transactionService.transfer("NB001", "NB001", new BigDecimal("100"), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transfer_shouldThrowInsufficientFunds_whenSourceHasNotEnough() {
        Account destination = Account.builder()
                .id(3L).uuid(UUID.randomUUID()).accountNumber("NB004")
                .type(AccountType.SAVINGS).currency("USD")
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(transactionRepository.findByReference(any())).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumberWithLock("NB001")).thenReturn(Optional.of(activeAccount));
        when(accountRepository.findByAccountNumberWithLock("NB004")).thenReturn(Optional.of(destination));

        assertThatThrownBy(() ->
                transactionService.transfer("NB001", "NB004", new BigDecimal("5000.00"), null, null))
                .isInstanceOf(InsufficientFundsException.class);
    }
}
