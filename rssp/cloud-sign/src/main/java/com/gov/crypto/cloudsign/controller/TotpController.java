package com.gov.crypto.cloudsign.controller;

import com.gov.crypto.cloudsign.model.UserTotp;
import com.gov.crypto.cloudsign.repository.UserTotpRepository;
import com.gov.crypto.cloudsign.service.TotpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/credentials/totp")
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

    // Helper record for response
    public record TotpSetupResponse(String secret, String qrUri) {}
}
