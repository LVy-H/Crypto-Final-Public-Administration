package com.gov.crypto.caauthority.registration.service;

import com.gov.crypto.caauthority.registration.dto.RegistrationRequest;
import com.gov.crypto.caauthority.registration.dto.RegistrationResponse;

public interface RegistrationService {
    RegistrationResponse registerUser(RegistrationRequest request, String authToken);
}
