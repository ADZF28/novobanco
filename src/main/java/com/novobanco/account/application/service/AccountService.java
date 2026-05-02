package com.novobanco.account.application.service;

import com.novobanco.account.application.annotation.UseCase;
import com.novobanco.account.application.port.in.AccountPort;
import com.novobanco.account.application.port.out.AccountRepositoryPort;
import com.novobanco.account.application.port.out.ClientRepositoryPort;
import com.novobanco.account.domain.enums.AccountStatus;
import com.novobanco.account.domain.enums.AccountType;
import com.novobanco.account.domain.exception.AccountNotFoundException;
import com.novobanco.account.domain.exception.ClientNotFoundException;
import com.novobanco.account.domain.exception.InsufficientInitialDepositException;
import com.novobanco.account.domain.model.Account;
import com.novobanco.account.domain.model.Client;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@UseCase
public class AccountService implements AccountPort {

    private static final BigDecimal CHECKING_MINIMUM_DEPOSIT = new BigDecimal("50.00");

    private final AccountRepositoryPort accountRepository;
    private final ClientRepositoryPort clientRepository;

    public AccountService(AccountRepositoryPort accountRepository,
                          ClientRepositoryPort clientRepository) {
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    @Transactional
    public Account createAccount(String identification, AccountType type, AccountStatus initialStatus, BigDecimal initialBalance) {
        Client client = clientRepository.findByIdentification(identification)
                .orElseThrow(() -> new ClientNotFoundException(identification));

        BigDecimal balance = initialBalance != null ? initialBalance : BigDecimal.ZERO;

        if (type == AccountType.CHECKING && balance.compareTo(CHECKING_MINIMUM_DEPOSIT) < 0) {
            throw new InsufficientInitialDepositException(CHECKING_MINIMUM_DEPOSIT);
        }

        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .client(client)
                .type(type)
                .currency("USD")
                .balance(balance)
                .status(initialStatus)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return accountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Account getAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    @Override
    @Transactional
    public Account updateStatus(String accountNumber, AccountStatus newStatus) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        Account updated = Account.builder()
                .id(account.getId())
                .uuid(account.getUuid())
                .accountNumber(account.getAccountNumber())
                .client(account.getClient())
                .type(account.getType())
                .currency(account.getCurrency())
                .balance(account.getBalance())
                .status(newStatus)
                .createdAt(account.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        return accountRepository.save(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> getAccountsByClientIdentification(String identification) {
        Client client = clientRepository.findByIdentification(identification)
                .orElseThrow(() -> new ClientNotFoundException(identification));
        return accountRepository.findByClientInternalId(client.getId());
    }

    private String generateAccountNumber() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(100_000_000_000_000L, 1_000_000_000_000_000L));
    }
}
