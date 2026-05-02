package com.novobanco.account.domain.exception;

import java.util.UUID;

public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(UUID reference) {
        super("Ya existe una transacción con la referencia: " + reference
              + ". La operación no fue procesada nuevamente (idempotencia).");
    }
}
