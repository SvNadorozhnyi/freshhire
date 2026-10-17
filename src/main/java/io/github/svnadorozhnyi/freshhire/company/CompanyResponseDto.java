package io.github.svnadorozhnyi.freshhire.company;

import java.time.Instant;

public record CompanyResponseDto(
        Long id,
        String name,
        AtsType atsType,
        String boardToken,
        boolean active,
        Instant lastCollectedAt,
        CollectionStatus lastCollectionStatus,
        Instant createdAt,
        Instant updatedAt) {}
