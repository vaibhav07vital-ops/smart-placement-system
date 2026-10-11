import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Single-file demonstration of a Smart Student Placement Management System.
 * Paste this entire file into any Java 17 online compiler and click Run.
 */
public class OnlinePlacementDemo {
    enum PlacementStatus { UNPLACED, SHORTLISTED, PLACED }
    enum ApplicationStatus { APPLIED, SHORTLISTED, SELECTED, REJECTED }

    static class Student {
        final String id, name, branch;
        final double cgpa;
        final Set<String> skills;
        PlacementStatus placementStatus = PlacementStatus.UNPLACED;

        Student(String id, String name, String branch, double cgpa, String... skills) {
            if (cgpa < 0 || cgpa > 10) throw new IllegalArgumentException("CGPA must be between 0 and 10.");
            this.id = id;
            this.name = name;
            this.branch = branch;
            this.cgpa = cgpa;
            this.skills = Arrays.stream(skills)
                    .map(skill -> skill.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
        }

        @Override public String toString() {
            return id + " | " + name + " | " + branch + " | CGPA " + cgpa
                    + " | " + skills + " | " + placementStatus;
        }
    }

    static class Company {
        final String id, name, role;
        final double minimumCgpa;
        final Set<String> eligibleBranches, requiredSkills;

        Company(String id, String name, String role, double minimumCgpa,
                Set<String> eligibleBranches, Set<String> requiredSkills) {
            this.id = id;
            this.name = name;
            this.role = role;
            this.minimumCgpa = minimumCgpa;
            this.eligibleBranches = normalise(eligibleBranches);
            this.requiredSkills = normalise(requiredSkills);
        }

        private static Set<String> normalise(Set<String> values) {
            return values.stream().map(value -> value.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        }
    }

    static class Application {
        final String studentId, companyId;
        ApplicationStatus status = ApplicationStatus.APPLIED;

        Application(String studentId, String companyId) {
            this.studentId = studentId;
            this.companyId = companyId;
        }

        @Override public String toString() {
            return studentId + " -> " + companyId + " : " + status;
        }
    }

    static class PlacementService {
        final Map<String, Student> students = new HashMap<>();
        final Map<String, Company> companies = new HashMap<>();
        final List<Application> applications = new ArrayList<>();

        void registerStudent(Student student) {
            if (students.putIfAbsent(student.id, student) != null) {
                throw new IllegalArgumentException("Duplicate student ID: " + student.id);
            }
        }

        void registerCompany(Company company) {
            if (companies.putIfAbsent(company.id, company) != null) {
                throw new IllegalArgumentException("Duplicate company ID: " + company.id);
            }
        }

        List<Student> eligibleStudents(String companyId) {
            Company company = getCompany(companyId);
            return students.values().stream()                 // Stream API
                    .filter(student -> isEligible(student, company))
                    .sorted(Comparator.comparingDouble((Student s) -> s.cgpa).reversed())
                    .collect(Collectors.toList());
        }

        boolean isEligible(Student student, Company company) {
            return student.cgpa >= company.minimumCgpa
                    && company.eligibleBranches.contains(student.branch.toLowerCase(Locale.ROOT))
                    && student.skills.containsAll(company.requiredSkills)
                    && student.placementStatus != PlacementStatus.PLACED;
        }

        void apply(String studentId, String companyId) {
            Student student = getStudent(studentId);
            Company company = getCompany(companyId);
            if (!isEligible(student, company)) {
                throw new IllegalArgumentException(student.name + " is not eligible for " + company.name + ".");
            }
            boolean alreadyApplied = applications.stream().anyMatch(application ->
                    application.studentId.equals(studentId) && application.companyId.equals(companyId));
            if (alreadyApplied) throw new IllegalStateException("Duplicate application is not allowed.");
            applications.add(new Application(studentId, companyId));
        }

        void shortlist(String studentId, String companyId) {
            Application application = getApplication(studentId, companyId);
            if (application.status != ApplicationStatus.APPLIED) throw new IllegalStateException("Applicant is not in APPLIED state.");
            application.status = ApplicationStatus.SHORTLISTED;
            getStudent(studentId).placementStatus = PlacementStatus.SHORTLISTED;
        }

        void select(String studentId, String companyId) {
            Application selected = getApplication(studentId, companyId);
            if (selected.status != ApplicationStatus.SHORTLISTED) throw new IllegalStateException("Only shortlisted applicants can be selected.");
            selected.status = ApplicationStatus.SELECTED;
            getStudent(studentId).placementStatus = PlacementStatus.PLACED;
            applications.stream()
                    .filter(application -> application.studentId.equals(studentId) && application != selected)
                    .filter(application -> application.status != ApplicationStatus.SELECTED)
                    .forEach(application -> application.status = ApplicationStatus.REJECTED);
        }

        Student getStudent(String id) {
            if (!students.containsKey(id)) throw new NoSuchElementException("Student not found: " + id);
            return students.get(id);
        }

        Company getCompany(String id) {
            if (!companies.containsKey(id)) throw new NoSuchElementException("Company not found: " + id);
            return companies.get(id);
        }

        Application getApplication(String studentId, String companyId) {
            return applications.stream()
                    .filter(application -> application.studentId.equals(studentId) && application.companyId.equals(companyId))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException("Application not found."));
        }
    }

    public static void main(String[] args) throws Exception {
        PlacementService service = new PlacementService();

        service.registerStudent(new Student("S101", "Aarav Sharma", "CSE", 8.8, "Java", "SQL", "DSA"));
        service.registerStudent(new Student("S102", "Diya Nair", "ISE", 8.1, "Java", "SQL", "Python"));
        service.registerStudent(new Student("S103", "Rohan Das", "ECE", 7.2, "C++", "Python"));
        service.registerStudent(new Student("S104", "Meera Iyer", "CSE", 6.4, "Java", "HTML"));

        service.registerCompany(new Company("C201", "NovaTech", "Backend Engineer", 7.5,
                Set.of("CSE", "ISE"), Set.of("Java", "SQL")));
        service.registerCompany(new Company("C202", "DataVista", "Data Analyst", 7.0,
                Set.of("CSE", "ISE", "ECE"), Set.of("Python")));

        System.out.println("=== SMART STUDENT PLACEMENT MANAGEMENT SYSTEM ===\n");

        // Multithreading: two independent reports run at the same time.
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<List<Student>> eligibilityTask = pool.submit(() -> service.eligibleStudents("C201"));
        Future<Long> totalStudentsTask = pool.submit(() -> service.students.values().stream().count());

        System.out.println("Eligible students for NovaTech (C201):");
        eligibilityTask.get().forEach(student -> System.out.println("  " + student));
        System.out.println("\nTotal registered students: " + totalStudentsTask.get());
        pool.shutdown();

        System.out.println("\n--- Application workflow ---");
        service.apply("S101", "C201");
        service.apply("S102", "C201");
        service.shortlist("S101", "C201");
        service.select("S101", "C201");
        service.applications.forEach(application -> System.out.println("  " + application));

        System.out.println("\n--- Exception handling demonstration ---");
        try {
            service.apply("S104", "C201"); // CGPA is below the required 7.5
        } catch (IllegalArgumentException exception) {
            System.out.println("Handled safely: " + exception.getMessage());
        }

        System.out.println("\n--- Final student status ---");
        service.students.values().stream().sorted(Comparator.comparing(student -> student.id))
                .forEach(student -> System.out.println("  " + student));
    }
}
