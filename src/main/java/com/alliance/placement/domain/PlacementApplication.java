package com.alliance.placement.domain;

import java.time.Instant;
import java.util.Objects;

/** A student's application to one company. */
public final class PlacementApplication {
    private final String studentId;
    private final String companyId;
    private final Instant appliedAt;
    private volatile ApplicationStatus status;

    public PlacementApplication(String studentId, String companyId) {
        this.studentId = Objects.requireNonNull(studentId);
        this.companyId = Objects.requireNonNull(companyId);
        this.appliedAt = Instant.now();
        this.status = ApplicationStatus.APPLIED;
    }

    public String studentId() { return studentId; }
    public String companyId() { return companyId; }
    public Instant appliedAt() { return appliedAt; }
    public ApplicationStatus status() { return status; }
    public void updateStatus(ApplicationStatus status) { this.status = Objects.requireNonNull(status); }

    @Override public String toString() {
        return "%s -> %s | %s | applied %s".formatted(studentId, companyId, status, appliedAt);
    }
}
