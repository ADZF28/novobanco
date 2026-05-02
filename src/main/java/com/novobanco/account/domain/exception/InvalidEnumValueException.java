package com.novobanco.account.domain.exception;

public class InvalidEnumValueException extends RuntimeException {

    public InvalidEnumValueException(String field, String value, String allowed) {
        super("El valor '" + value + "' no es válido para '" + field + "'. Valores permitidos: " + allowed);
    }
}
