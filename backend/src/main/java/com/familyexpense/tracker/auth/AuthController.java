package com.familyexpense.tracker.auth;

import com.familyexpense.tracker.profile.Profile;
import com.familyexpense.tracker.profile.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ProfileService profileService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            ProfileService profileService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder
    ) {
        this.profileService = profileService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Profile profile = profileService.getProfileById(request.profileId());
            if (!profile.isActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Profile is inactive");
            }
            if (!passwordEncoder.matches(request.pin(), profile.getPinHash())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid PIN");
            }
            String token = jwtService.generateToken(profile.getId(), profile.getName());
            return ResponseEntity.ok(new LoginResponse(token, profile.getId(), profile.getName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Profile ID");
        }
    }
}
