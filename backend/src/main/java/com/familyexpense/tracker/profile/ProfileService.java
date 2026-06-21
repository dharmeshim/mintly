package com.familyexpense.tracker.profile;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(ProfileRepository profileRepository, PasswordEncoder passwordEncoder) {
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Profile> getAllProfiles() {
        return profileRepository.findAll();
    }

    public Profile createProfile(String name, String pin) {
        if (pin == null || pin.length() < 4 || pin.length() > 6) {
            throw new IllegalArgumentException("PIN must be between 4 and 6 digits");
        }
        String pinHash = passwordEncoder.encode(pin);
        Profile profile = new Profile(name, pinHash);
        return profileRepository.save(profile);
    }

    public Profile updateProfile(Long id, String name, Boolean active) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
        if (name != null) {
            profile.setName(name);
        }
        if (active != null) {
            profile.setActive(active);
        }
        return profileRepository.save(profile);
    }

    public Profile getProfileById(Long id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
    }
}
