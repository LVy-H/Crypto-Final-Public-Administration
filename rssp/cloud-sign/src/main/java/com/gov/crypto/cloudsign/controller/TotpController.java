package com.gov.crypto.cloudsign.controller;

import com.gov.crypto.cloudsign.model.UserTotp;
import com.gov.crypto.cloudsign.repository.UserTotpRepository;
import com.gov.crypto.cloudsign.service.TotpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/credentials/totp")
public class TotpController {

    private final TotpService totpService;
    private final UserTotpRepository userTotpRepository;

    public TotpController(TotpService totpService, UserTotpRepository userTotpRepository) {
        this.totpService = totpService;
        this.userTotpRepository = userTotpRepository;
    }

    @PostMapping("/setup")
    public ResponseEntity<?> setupTotp(Principal principal) {
        // TODO: Ensure proper Principal injection integration
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized: Principal not found");
        }
        String username = principal.getName();

        String secret = totpService.generateSecret();
        String qrUri = totpService.getQrCodeUri(secret, username);

        // Save secret (Upsert)
        UserTotp userTotp = userTotpRepository.findByUsername(username)
                .orElse(new UserTotp(username, secret));
        userTotp.setSecretKey(secret);
        userTotpRepository.save(userTotp);

        return ResponseEntity.ok(new TotpSetupResponse(secret, qrUri));
    }

    public record TotpSetupResponse(String secret, String qrUri) {
    }

    public record TotpVerifyRequest(String username, String code) {
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyTotp(@RequestBody TotpVerifyRequest request) {
        return userTotpRepository.findByUsername(request.username())
                .map(totp -> {
                    try {
                        int code = Integer.parseInt(request.code());
                        boolean valid = totpService.verifyCode(totp.getSecretKey(), code);
                        if (valid) {
                            return ResponseEntity.ok().build();
                        } else {
                            return ResponseEntity.badRequest().body("Invalid Code");
                        }
                    } catch (NumberFormatException e) {
                        return ResponseEntity.badRequest().body("Invalid Code Format");
                    }
                })
                .orElse(ResponseEntity.status(404).body("TOTP not set up"));
    }
}
