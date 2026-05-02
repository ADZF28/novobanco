package com.novobanco.account.domain.exception;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(UUID reference) {
        super("No existe ninguna transacción con la referencia: " + reference);
    }
}
