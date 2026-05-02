package com.novobanco.account.domain.exception;

public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(String identification) {
        super("Cliente no encontrado con identificación: " + identification);
    }
}
