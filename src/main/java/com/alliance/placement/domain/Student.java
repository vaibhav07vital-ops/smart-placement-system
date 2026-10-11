package com.alliance.placement.domain;

import com.alliance.placement.exception.ValidationException;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

/** A candidate registered with the placement cell. */
public final class Student {
    private final String id;
    private final String name;
    private final String branch;
    private final double cgpa;
    private final Set<String> skills;
    private final AtomicReference<PlacementStatus> placementStatus = new AtomicReference<>(PlacementStatus.UNPLACED);

    public Student(String id, String name, String branch, double cgpa, Collection<String> skills) {
        this.id = requireText(id, "Student ID");
        this.name = requireText(name, "Student name");
        this.branch = requireText(branch, "Branch");
        if (cgpa < 0.0 || cgpa > 10.0) {
            throw new ValidationException("CGPA must be between 0.0 and 10.0.");
        }
        this.cgpa = cgpa;
        this.skills = normaliseSkills(skills);
    }

    public String id() { return id; }
    public String name() { return name; }
    public String branch() { return branch; }
    public double cgpa() { return cgpa; }
    public Set<String> skills() { return Set.copyOf(skills); }
    public PlacementStatus placementStatus() { return placementStatus.get(); }

    public void updatePlacementStatus(PlacementStatus status) {
        placementStatus.set(Objects.requireNonNull(status, "Placement status is required."));
    }

    public boolean hasAllSkills(Collection<String> requiredSkills) {
        return skills.containsAll(normaliseSkills(requiredSkills));
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(label + " cannot be blank.");
        }
        return value.trim();
    }

    private static Set<String> normaliseSkills(Collection<String> input) {
        if (input == null) {
            throw new ValidationException("Skills are required (an empty set is allowed).");
        }
        Set<String> result = new TreeSet<>();
        input.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .forEach(result::add);
        return result;
    }

    @Override
    public String toString() {
        return "%s | %s | %s | CGPA %.2f | %s | %s".formatted(id, name, branch, cgpa, skills, placementStatus);
    }
}
