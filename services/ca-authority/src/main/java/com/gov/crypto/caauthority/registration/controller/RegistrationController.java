package com.gov.crypto.caauthority.registration.controller;

import com.gov.crypto.caauthority.registration.dto.RegistrationRequest;
import com.gov.crypto.caauthority.registration.dto.RegistrationResponse;
import com.gov.crypto.caauthority.registration.service.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ra")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/request")
    public ResponseEntity<RegistrationResponse> submitRequest(@RequestBody RegistrationRequest request) {
        return ResponseEntity.ok(registrationService.registerUser(request));
    }
}
