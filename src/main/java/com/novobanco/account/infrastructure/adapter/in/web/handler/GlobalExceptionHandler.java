package com.novobanco.account.infrastructure.adapter.in.web.handler;

import com.novobanco.account.domain.exception.AccountNotFoundException;
import com.novobanco.account.domain.exception.AccountNotOperableException;
import com.novobanco.account.domain.exception.ClientNotFoundException;
import com.novobanco.account.domain.exception.DuplicateTransactionException;
import com.novobanco.account.domain.exception.InsufficientFundsException;
import com.novobanco.account.domain.exception.InsufficientInitialDepositException;
import com.novobanco.account.domain.exception.InvalidEnumValueException;
import com.novobanco.account.domain.exception.TransactionNotFoundException;
import com.novobanco.account.infrastructure.adapter.in.web.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleAccountNotFound(AccountNotFoundException ex, HttpServletRequest req) {
        return ApiErrorResponse.of(404, ex.getMessage());
    }

    @ExceptionHandler(ClientNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleClientNotFound(ClientNotFoundException ex, HttpServletRequest req) {
        return ApiErrorResponse.of(404, ex.getMessage());
    }

    @ExceptionHandler(AccountNotOperableException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ApiErrorResponse handleAccountNotOperable(AccountNotOperableException ex, HttpServletRequest req) {
        return ApiErrorResponse.of(422, ex.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ApiErrorResponse handleInsufficientFunds(InsufficientFundsException ex, HttpServletRequest req) {
        return ApiErrorResponse.of(422, ex.getMessage());
    }

    @ExceptionHandler(InsufficientInitialDepositException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ApiErrorResponse handleInsufficientInitialDeposit(InsufficientInitialDepositException ex, HttpServletRequest req) {
        return ApiErrorResponse.of(422, ex.getMessage());
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleTransactionNotFound(TransactionNotFoundException ex, HttpServletRequest req) {
        return ApiErrorResponse.of(404, ex.getMessage());
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleDuplicate(DuplicateTransactionException ex, HttpServletRequest req) {
        return ApiErrorResponse.of(409, ex.getMessage());
    }

    @ExceptionHandler(InvalidEnumValueException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleInvalidEnumValue(InvalidEnumValueException ex, HttpServletRequest req) {
        return ApiErrorResponse.of(400, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return ApiErrorResponse.of(400, "El cuerpo de la solicitud es inválido o tiene un formato incorrecto");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return ApiErrorResponse.of(400, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String firstError = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst()
                .orElse("La request contiene campos inválidos");
        return ApiErrorResponse.of(400, firstError);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleGeneric(Exception ex, HttpServletRequest req) {
        return ApiErrorResponse.of(500, "Error interno del servidor. Por favor contacte soporte.");
    }
}
