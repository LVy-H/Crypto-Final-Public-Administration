package com.gov.crypto.caauthority.registration.service;

import com.gov.crypto.caauthority.registration.dto.CaRegistrationRequest;
import com.gov.crypto.caauthority.registration.dto.RegistrationResponse;

public interface RegistrationService {
    RegistrationResponse registerUser(CaRegistrationRequest request, String authToken);
}
