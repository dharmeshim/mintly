package com.familyexpense.tracker.auth;

import jakarta.validation.constraints.NotNull;

public record LoginRequest(
    @NotNull Long profileId,
    @NotNull String pin
) {}
