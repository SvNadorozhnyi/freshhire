package io.github.svnadorozhnyi.freshhire.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "companies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "ats_type", nullable = false, length = 32)
    private AtsType atsType;

    @Column(name = "board_token", nullable = false, length = 255)
    private String boardToken;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "last_collected_at")
    private Instant lastCollectedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_collection_status", length = 32)
    private CollectionStatus lastCollectionStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Company(String name, AtsType atsType, String boardToken, Instant now) {
        this.name = name;
        this.atsType = atsType;
        this.boardToken = boardToken;
        this.createdAt = now;
        this.updatedAt = now;
        this.active = true;
    }

    public void updateDetails(String name, AtsType atsType, String boardToken, Instant now) {
        this.name = name;
        this.atsType = atsType;
        this.boardToken = boardToken;
        this.updatedAt = now;
    }

    public void activate(Instant now) {
        this.active = true;
        this.updatedAt = now;
    }

    public void deactivate(Instant now) {
        this.active = false;
        this.updatedAt = now;
    }
}
