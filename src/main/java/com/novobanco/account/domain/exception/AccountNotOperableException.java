package com.novobanco.account.domain.exception;

import com.novobanco.account.domain.enums.AccountStatus;

public class AccountNotOperableException extends RuntimeException {

    private final String accountNumber;
    private final AccountStatus status;

    public AccountNotOperableException(String accountNumber, AccountStatus status) {
        super(buildMessage(accountNumber, status));
        this.accountNumber = accountNumber;
        this.status = status;
    }

    private static String buildMessage(String accountNumber, AccountStatus status) {
        return switch (status) {
            case BLOCKED -> "La cuenta " + accountNumber + " está bloqueada y no puede operar. Contacte al banco.";
            case CLOSED  -> "La cuenta " + accountNumber + " está cerrada y no acepta operaciones.";
            default      -> "La cuenta " + accountNumber + " no está habilitada para operar (estado: " + status + ").";
        };
    }

    public String getAccountNumber() { return accountNumber; }
    public AccountStatus getStatus() { return status; }
}
