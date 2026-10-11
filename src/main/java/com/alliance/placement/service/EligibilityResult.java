package com.alliance.placement.service;

import com.alliance.placement.domain.Student;

import java.util.List;

public record EligibilityResult(String companyId, List<Student> eligibleStudents) {
    public EligibilityResult { eligibleStudents = List.copyOf(eligibleStudents); }
}
