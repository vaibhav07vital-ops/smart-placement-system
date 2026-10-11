package com.alliance.placement.domain;

import com.alliance.placement.exception.ValidationException;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Placement opportunity and its eligibility criteria. */
public record Company(String id, String name, String jobRole, double minimumCgpa,
                      Set<String> eligibleBranches, Set<String> requiredSkills) {
    public Company {
        id = requireText(id, "Company ID");
        name = requireText(name, "Company name");
        jobRole = requireText(jobRole, "Job role");
        if (minimumCgpa < 0.0 || minimumCgpa > 10.0) {
            throw new ValidationException("Minimum CGPA must be between 0.0 and 10.0.");
        }
        eligibleBranches = normalise(eligibleBranches, "Eligible branches");
        requiredSkills = normalise(requiredSkills, "Required skills");
    }

    public static Company of(String id, String name, String jobRole, double minimumCgpa,
                             Collection<String> branches, Collection<String> skills) {
        return new Company(id, name, jobRole, minimumCgpa, new TreeSet<>(branches), new TreeSet<>(skills));
    }

    public boolean acceptsBranch(String branch) {
        return eligibleBranches.contains(branch.trim().toLowerCase(Locale.ROOT));
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new ValidationException(label + " cannot be blank.");
        return value.trim();
    }

    private static Set<String> normalise(Collection<String> values, String label) {
        if (values == null || values.isEmpty()) throw new ValidationException(label + " cannot be empty.");
        Set<String> result = new TreeSet<>();
        values.stream().filter(Objects::nonNull).map(String::trim).filter(v -> !v.isEmpty())
                .map(v -> v.toLowerCase(Locale.ROOT)).forEach(result::add);
        if (result.isEmpty()) throw new ValidationException(label + " cannot be empty.");
        return Set.copyOf(result);
    }

    @Override public String toString() {
        return "%s | %s - %s | min CGPA %.2f | branches %s | skills %s"
                .formatted(id, name, jobRole, minimumCgpa, eligibleBranches, requiredSkills);
    }
}
