package com.gov.crypto.validationservice.controller;

import com.gov.crypto.validationservice.dto.VerifyRequest;
import com.gov.crypto.validationservice.dto.VerifyResponse;
import com.gov.crypto.validationservice.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/validation")
public class ValidationController {

    private final ValidationService validationService;

    public ValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/verify")
    public ResponseEntity<VerifyResponse> verifySignature(@RequestBody VerifyRequest request) {
        return ResponseEntity.ok(validationService.verifySignature(request));
    }
}
