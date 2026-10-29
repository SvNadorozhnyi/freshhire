package io.github.svnadorozhnyi.freshhire.collector;

import java.time.Instant;
import java.util.Objects;

public record CollectedPosting(String externalId, String title, String location, String url, Instant publishedAt) {

    public CollectedPosting {
        Objects.requireNonNull(externalId, "externalId must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(url, "url must not be null");
    }
}
