package com.familyexpense.tracker.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProfileRequest(
    @NotBlank String name,
    @NotBlank @Size(min = 4, max = 6) String pin
) {}
