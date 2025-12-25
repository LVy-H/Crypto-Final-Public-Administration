package com.gov.crypto.raservice.service;

import com.gov.crypto.raservice.dto.RegistrationRequest;
import com.gov.crypto.raservice.dto.RegistrationResponse;

public interface RegistrationService {
    RegistrationResponse registerUser(RegistrationRequest request);
}
