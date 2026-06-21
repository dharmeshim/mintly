package com.familyexpense.tracker.auth;

public record LoginResponse(
    String token,
    Long profileId,
    String name
) {}
