package com.familyexpense.tracker.profile;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<List<ProfileDto>> getProfiles() {
        List<ProfileDto> dtos = profileService.getAllProfiles().stream()
                .map(ProfileDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<ProfileDto> createProfile(@Valid @RequestBody CreateProfileRequest request) {
        Profile profile = profileService.createProfile(request.name(), request.pin());
        return ResponseEntity.ok(ProfileDto.fromEntity(profile));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProfileDto> updateProfile(
            @PathVariable Long id,
            @RequestBody UpdateProfileRequest request
    ) {
        Profile profile = profileService.updateProfile(id, request.name(), request.active());
        return ResponseEntity.ok(ProfileDto.fromEntity(profile));
    }
}
