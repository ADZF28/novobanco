package com.novobanco.account.domain.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String accountNumber, BigDecimal requested, BigDecimal available) {
        super(String.format(
                "Fondos insuficientes en cuenta %s. Solicitado: %.2f, disponible: %.2f",
                accountNumber, requested, available));
    }
}
