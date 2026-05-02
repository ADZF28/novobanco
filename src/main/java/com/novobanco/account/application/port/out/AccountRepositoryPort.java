package com.novobanco.account.application.port.out;

import com.novobanco.account.domain.model.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepositoryPort {
    Account save(Account account);
    Optional<Account> findByAccountNumber(String accountNumber);
    Optional<Account> findByAccountNumberWithLock(String accountNumber);
    List<Account> findByClientInternalId(Long clientId);
}
