package com.novobanco.account.application.port.in;

import com.novobanco.account.domain.enums.AccountStatus;
import com.novobanco.account.domain.enums.AccountType;
import com.novobanco.account.domain.model.Account;

import java.math.BigDecimal;
import java.util.List;

public interface AccountPort {
    Account createAccount(String identification, AccountType type, AccountStatus initialStatus, BigDecimal initialBalance);
    Account getAccount(String accountNumber);
    Account updateStatus(String accountNumber, AccountStatus newStatus);
    List<Account> getAccountsByClientIdentification(String identification);
}
