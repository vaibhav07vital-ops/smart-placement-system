package com.alliance.placement.service;

import com.alliance.placement.domain.ApplicationStatus;
import com.alliance.placement.domain.Company;
import com.alliance.placement.domain.PlacementStatus;
import com.alliance.placement.domain.Student;
import com.alliance.placement.exception.DuplicateApplicationException;
import com.alliance.placement.exception.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlacementServiceTest {
    private final PlacementService service = new PlacementService();

    @AfterEach void shutdown() { service.close(); }

    @Test void eligibilityRequiresCgpaBranchAndEverySkill() {
        service.registerStudent(new Student("S1", "Anika", "CSE", 8.0, List.of("Java", "SQL")));
        service.registerStudent(new Student("S2", "Bala", "ECE", 8.5, List.of("Java", "SQL")));
        service.registerStudent(new Student("S3", "Charu", "CSE", 8.5, List.of("Java")));
        service.registerCompany(Company.of("C1", "Acme", "Engineer", 7.5, List.of("CSE"), List.of("Java", "SQL")));

        assertEquals(List.of("S1"), service.getEligibleStudents("C1").stream().map(Student::id).toList());
    }

    @Test void applicationMustBeEligibleAndUnique() {
        registerEligiblePair();
        service.apply("S1", "C1");
        assertThrows(DuplicateApplicationException.class, () -> service.apply("S1", "C1"));
        assertThrows(ValidationException.class, () -> service.apply("S2", "C1"));
    }

    @Test void selectionUpdatesCandidateAndRejectsOtherOpenApplications() {
        registerEligiblePair();
        service.registerCompany(Company.of("C2", "Beta", "Engineer", 7.0, List.of("CSE"), List.of("Java")));
        service.apply("S1", "C1");
        service.apply("S1", "C2");
        service.shortlist("S1", "C1");
        service.select("S1", "C1");

        assertEquals(PlacementStatus.PLACED, service.findStudent("S1").placementStatus());
        assertEquals(ApplicationStatus.SELECTED, service.getApplicationsForCompany("C1").get(0).status());
        assertEquals(ApplicationStatus.REJECTED, service.getApplicationsForCompany("C2").get(0).status());
    }

    private void registerEligiblePair() {
        service.registerStudent(new Student("S1", "Anika", "CSE", 8.0, List.of("Java", "SQL")));
        service.registerStudent(new Student("S2", "Bala", "CSE", 6.0, List.of("Java", "SQL")));
        service.registerCompany(Company.of("C1", "Acme", "Engineer", 7.5, List.of("CSE"), List.of("Java", "SQL")));
    }
}
