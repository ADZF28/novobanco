package com.novobanco.account.domain.util;

import com.novobanco.account.domain.enums.AccountStatus;
import com.novobanco.account.domain.exception.AccountNotOperableException;
import com.novobanco.account.domain.exception.InsufficientFundsException;
import com.novobanco.account.domain.model.Account;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class AccountDomainService {

    private AccountDomainService() {}

    public static void credit(Account account, BigDecimal amount) {
        validateOperable(account);
        validatePositiveAmount(amount);
        account.setBalance(account.getBalance().add(amount));
        account.setUpdatedAt(LocalDateTime.now());
    }

    public static void debit(Account account, BigDecimal amount) {
        validateOperable(account);
        validatePositiveAmount(amount);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(account.getAccountNumber(), amount, account.getBalance());
        }
        account.setBalance(account.getBalance().subtract(amount));
        account.setUpdatedAt(LocalDateTime.now());
    }

    public static void validateOperable(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotOperableException(account.getAccountNumber(), account.getStatus());
        }
    }

    private static void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
    }
}
