package com.alliance.placement.service;

import com.alliance.placement.domain.ApplicationStatus;
import com.alliance.placement.domain.Company;
import com.alliance.placement.domain.PlacementApplication;
import com.alliance.placement.domain.PlacementStatus;
import com.alliance.placement.domain.Student;
import com.alliance.placement.exception.DuplicateApplicationException;
import com.alliance.placement.exception.RecordNotFoundException;
import com.alliance.placement.exception.ValidationException;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/** Thread-safe in-memory service that implements placement-cell business rules. */
public final class PlacementService implements AutoCloseable {
    private final Map<String, Student> students = new ConcurrentHashMap<>();
    private final Map<String, Company> companies = new ConcurrentHashMap<>();
    private final Map<String, PlacementApplication> applications = new ConcurrentHashMap<>();
    private final ExecutorService workerPool = Executors.newFixedThreadPool(2);

    public Student registerStudent(Student student) {
        Objects.requireNonNull(student, "Student is required.");
        if (students.putIfAbsent(student.id(), student) != null) {
            throw new ValidationException("Student ID already exists: " + student.id());
        }
        return student;
    }

    public Company registerCompany(Company company) {
        Objects.requireNonNull(company, "Company is required.");
        if (companies.putIfAbsent(company.id(), company) != null) {
            throw new ValidationException("Company ID already exists: " + company.id());
        }
        return company;
    }

    public Student findStudent(String studentId) { return requireStudent(studentId); }
    public Company findCompany(String companyId) { return requireCompany(companyId); }

    public List<Student> getEligibleStudents(String companyId) {
        Company company = requireCompany(companyId);
        return students.values().stream()
                .filter(student -> isEligible(student, company))
                .sorted(Comparator.comparingDouble(Student::cgpa).reversed().thenComparing(Student::id))
                .toList();
    }

    public boolean isEligible(Student student, Company company) {
        return student.cgpa() >= company.minimumCgpa()
                && company.acceptsBranch(student.branch())
                && student.hasAllSkills(company.requiredSkills())
                && student.placementStatus() != PlacementStatus.PLACED;
    }

    public synchronized PlacementApplication apply(String studentId, String companyId) {
        Student student = requireStudent(studentId);
        Company company = requireCompany(companyId);
        if (!isEligible(student, company)) {
            throw new ValidationException(studentId + " is not eligible for " + companyId + ".");
        }
        String key = applicationKey(studentId, companyId);
        PlacementApplication application = new PlacementApplication(studentId, companyId);
        if (applications.putIfAbsent(key, application) != null) {
            throw new DuplicateApplicationException("Application already exists for student " + studentId + " and company " + companyId + ".");
        }
        return application;
    }

    public synchronized void shortlist(String studentId, String companyId) { updateApplication(studentId, companyId, ApplicationStatus.SHORTLISTED); }

    public synchronized void select(String studentId, String companyId) {
        PlacementApplication application = getApplication(studentId, companyId);
        if (application.status() != ApplicationStatus.SHORTLISTED) {
            throw new ValidationException("Only shortlisted candidates can be selected.");
        }
        application.updateStatus(ApplicationStatus.SELECTED);
        requireStudent(studentId).updatePlacementStatus(PlacementStatus.PLACED);
        applications.values().stream()
                .filter(other -> other.studentId().equals(studentId) && other != application
                        && other.status() != ApplicationStatus.SELECTED)
                .forEach(other -> other.updateStatus(ApplicationStatus.REJECTED));
    }

    public List<PlacementApplication> getApplicationsForStudent(String studentId) {
        requireStudent(studentId);
        return applications.values().stream().filter(a -> a.studentId().equals(studentId))
                .sorted(Comparator.comparing(PlacementApplication::appliedAt)).toList();
    }

    public List<PlacementApplication> getApplicationsForCompany(String companyId) {
        requireCompany(companyId);
        return applications.values().stream().filter(a -> a.companyId().equals(companyId))
                .sorted(Comparator.comparing(PlacementApplication::appliedAt)).toList();
    }

    public List<Student> searchStudents(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase();
        return students.values().stream().filter(s -> s.name().toLowerCase().contains(needle)
                        || s.id().toLowerCase().contains(needle) || s.branch().toLowerCase().contains(needle))
                .sorted(Comparator.comparing(Student::id)).toList();
    }

    public List<Student> allStudents() { return students.values().stream().sorted(Comparator.comparing(Student::id)).toList(); }
    public List<Company> allCompanies() { return companies.values().stream().sorted(Comparator.comparing(Company::id)).toList(); }

    /** Runs independent eligibility and aggregate-report tasks concurrently. */
    public CompletableFuture<EligibilityResult> generateEligibilityAsync(String companyId) {
        return CompletableFuture.supplyAsync(() -> new EligibilityResult(companyId, getEligibleStudents(companyId)), workerPool);
    }

    public CompletableFuture<PlacementReport> generateReportAsync() {
        return CompletableFuture.supplyAsync(() -> {
            Map<PlacementStatus, Long> statusCounts = students.values().stream()
                    .collect(Collectors.groupingBy(Student::placementStatus, () -> new EnumMap<>(PlacementStatus.class), Collectors.counting()));
            return new PlacementReport(students.size(), companies.size(), applications.size(), statusCounts);
        }, workerPool);
    }

    private void updateApplication(String studentId, String companyId, ApplicationStatus newStatus) {
        PlacementApplication application = getApplication(studentId, companyId);
        if (application.status() != ApplicationStatus.APPLIED) {
            throw new ValidationException("Only newly applied candidates can be shortlisted.");
        }
        application.updateStatus(newStatus);
        requireStudent(studentId).updatePlacementStatus(PlacementStatus.SHORTLISTED);
    }

    private PlacementApplication getApplication(String studentId, String companyId) {
        requireStudent(studentId); requireCompany(companyId);
        PlacementApplication application = applications.get(applicationKey(studentId, companyId));
        if (application == null) throw new RecordNotFoundException("No application exists for " + studentId + " and " + companyId + ".");
        return application;
    }

    private Student requireStudent(String id) {
        Student student = students.get(id);
        if (student == null) throw new RecordNotFoundException("Student not found: " + id);
        return student;
    }

    private Company requireCompany(String id) {
        Company company = companies.get(id);
        if (company == null) throw new RecordNotFoundException("Company not found: " + id);
        return company;
    }

    private static String applicationKey(String studentId, String companyId) { return studentId + "::" + companyId; }
    @Override public void close() { workerPool.shutdown(); }
}
