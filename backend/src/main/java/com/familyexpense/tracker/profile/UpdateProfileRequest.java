package com.familyexpense.tracker.profile;

public record UpdateProfileRequest(
    String name,
    Boolean active
) {}
