package com.novobanco.account.domain.exception;

import java.math.BigDecimal;

public class InsufficientInitialDepositException extends RuntimeException {

    public InsufficientInitialDepositException(BigDecimal minimum) {
        super("Las cuentas corrientes requieren un depósito inicial mínimo de $" + minimum);
    }
}
