package com.novobanco.account.application.port.in;

import com.novobanco.account.domain.enums.IdentificationType;
import com.novobanco.account.domain.model.Client;

public interface ClientPort {
    Client createClient(String fullName, String email, String identification,
                        IdentificationType typeIdentification, String phone, String address, String gender);
    Client getClient(String identification);
}
