package com.novobanco.account.application.service;

import com.novobanco.account.application.annotation.UseCase;
import com.novobanco.account.application.port.in.TransactionPort;
import com.novobanco.account.application.port.out.AccountRepositoryPort;
import com.novobanco.account.application.port.out.ClientRepositoryPort;
import com.novobanco.account.application.port.out.TransactionRepositoryPort;
import com.novobanco.account.domain.exception.AccountNotFoundException;
import com.novobanco.account.domain.exception.ClientNotFoundException;
import com.novobanco.account.domain.exception.DuplicateTransactionException;
import com.novobanco.account.domain.exception.TransactionNotFoundException;
import com.novobanco.account.domain.model.Account;
import com.novobanco.account.domain.model.Client;
import com.novobanco.account.domain.model.Transaction;
import com.novobanco.account.domain.util.AccountDomainService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@UseCase
public class TransactionService implements TransactionPort {

    private final AccountRepositoryPort accountRepository;
    private final TransactionRepositoryPort transactionRepository;
    private final ClientRepositoryPort clientRepository;

    public TransactionService(AccountRepositoryPort accountRepository,
                              TransactionRepositoryPort transactionRepository,
                              ClientRepositoryPort clientRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    @Transactional
    public Transaction deposit(String accountNumber, BigDecimal amount, UUID reference, String description) {
        UUID ref = resolveReference(reference);

        if (transactionRepository.findByReference(ref).isPresent()) {
            throw new DuplicateTransactionException(ref);
        }

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        AccountDomainService.credit(account, amount);
        accountRepository.save(account);

        Transaction tx = Transaction.createDeposit(account.getId(), amount, ref, description);
        return transactionRepository.save(tx);
    }

    @Override
    @Transactional
    public Transaction withdraw(String accountNumber, BigDecimal amount, UUID reference, String description) {
        UUID ref = resolveReference(reference);

        if (transactionRepository.findByReference(ref).isPresent()) {
            throw new DuplicateTransactionException(ref);
        }

        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        AccountDomainService.debit(account, amount);
        accountRepository.save(account);

        Transaction tx = Transaction.createWithdrawal(account.getId(), amount, ref, description);
        return transactionRepository.save(tx);
    }

    @Override
    @Transactional
    public List<Transaction> transfer(String sourceAccountNumber, String destinationAccountNumber,
                                      BigDecimal amount, UUID reference, String description) {
        if (sourceAccountNumber.equals(destinationAccountNumber)) {
            throw new IllegalArgumentException("No se puede transferir a la misma cuenta");
        }

        UUID ref = resolveReference(reference);
        if (transactionRepository.findByReference(ref).isPresent()) {
            throw new DuplicateTransactionException(ref);
        }

        Account first;
        Account second;
        boolean sourceIsFirst = sourceAccountNumber.compareTo(destinationAccountNumber) < 0;

        if (sourceIsFirst) {
            first  = lockAccount(sourceAccountNumber);
            second = lockAccount(destinationAccountNumber);
        } else {
            first  = lockAccount(destinationAccountNumber);
            second = lockAccount(sourceAccountNumber);
        }

        Account source      = sourceIsFirst ? first : second;
        Account destination = sourceIsFirst ? second : first;

        UUID transferReference = UUID.randomUUID();

        AccountDomainService.debit(source, amount);
        AccountDomainService.credit(destination, amount);

        accountRepository.save(source);
        accountRepository.save(destination);

        Transaction debit = transactionRepository.save(
                Transaction.createTransferDebit(
                        source.getId(), amount, ref, transferReference, description));

        Transaction credit = transactionRepository.save(
                Transaction.createTransferCredit(
                        destination.getId(), amount, UUID.randomUUID(), transferReference, description));

        return List.of(debit, credit);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Transaction> getHistory(String accountNumber, Pageable pageable) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        return transactionRepository.findByAccountId(account.getId(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Transaction findByReference(UUID reference) {
        return transactionRepository.findByReference(reference)
                .orElseThrow(() -> new TransactionNotFoundException(reference));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getOutgoingTransfers(String identification, LocalDate startDate, LocalDate endDate) {
        Client client = clientRepository.findByIdentification(identification)
                .orElseThrow(() -> new ClientNotFoundException(identification));

        List<Long> accountIds = accountRepository.findByClientInternalId(client.getId())
                .stream()
                .map(Account::getId)
                .toList();

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.plusDays(1).atStartOfDay();

        return transactionRepository.findOutgoingTransfers(accountIds, from, to);
    }

    private Account lockAccount(String accountNumber) {
        return accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private UUID resolveReference(UUID provided) {
        return provided != null ? provided : UUID.randomUUID();
    }
}
