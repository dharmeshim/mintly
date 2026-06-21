package com.familyexpense.tracker.profile;

import java.time.LocalDateTime;

public record ProfileDto(
    Long id,
    String name,
    boolean active,
    LocalDateTime createdAt
) {
    public static ProfileDto fromEntity(Profile profile) {
        return new ProfileDto(
            profile.getId(),
            profile.getName(),
            profile.isActive(),
            profile.getCreatedAt()
        );
    }
}
