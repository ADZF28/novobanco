package com.novobanco.account.domain.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountNumber) {
        super("Cuenta no encontrada: " + accountNumber);
    }
}
