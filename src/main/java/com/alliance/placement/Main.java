package com.alliance.placement;

import com.alliance.placement.domain.Company;
import com.alliance.placement.domain.PlacementApplication;
import com.alliance.placement.domain.Student;
import com.alliance.placement.exception.DuplicateApplicationException;
import com.alliance.placement.exception.RecordNotFoundException;
import com.alliance.placement.exception.ValidationException;
import com.alliance.placement.service.EligibilityResult;
import com.alliance.placement.service.PlacementReport;
import com.alliance.placement.service.PlacementService;
import com.alliance.placement.util.Input;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletionException;

/** Console entry point for the Smart Student Placement Management System. */
public final class Main {
    private final PlacementService service = new PlacementService();
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) { new Main().run(); }

    private void run() {
        System.out.println("\n=== Smart Student Placement Management System ===");
        try {
            boolean running = true;
            while (running) {
                printMenu();
                System.out.print("Choose an option: ");
                String choice = scanner.nextLine().trim();
                try {
                    switch (choice) {
                        case "1" -> loadDemoData();
                        case "2" -> registerStudent();
                        case "3" -> registerCompany();
                        case "4" -> listStudents();
                        case "5" -> listCompanies();
                        case "6" -> showEligibleStudents();
                        case "7" -> apply();
                        case "8" -> shortlist();
                        case "9" -> select();
                        case "10" -> showStudentApplications();
                        case "11" -> searchStudents();
                        case "12" -> runConcurrentReport();
                        case "0" -> running = false;
                        default -> System.out.println("Unknown option. Please choose a menu number.");
                    }
                } catch (ValidationException | RecordNotFoundException | DuplicateApplicationException exception) {
                    System.out.println("Operation failed: " + exception.getMessage());
                } catch (CompletionException exception) {
                    System.out.println("Background task failed: " + exception.getCause().getMessage());
                }
            }
        } finally {
            service.close();
            scanner.close();
            System.out.println("Placement system closed.");
        }
    }

    private void printMenu() {
        System.out.println("""

                1. Load demonstration data
                2. Register student
                3. Register company
                4. View students
                5. View companies
                6. Check eligible students for a company
                7. Apply to a company
                8. Shortlist an applicant
                9. Mark applicant as selected
                10. View a student's applications
                11. Search students
                12. Run concurrent eligibility and placement report
                0. Exit""");
    }

    private void loadDemoData() {
        if (!service.allStudents().isEmpty() || !service.allCompanies().isEmpty()) {
            throw new ValidationException("Demo data can only be loaded into an empty system.");
        }
        service.registerStudent(new Student("S101", "Aarav Sharma", "CSE", 8.8, List.of("Java", "SQL", "DSA", "Spring")));
        service.registerStudent(new Student("S102", "Diya Nair", "ISE", 8.1, List.of("Java", "SQL", "Python")));
        service.registerStudent(new Student("S103", "Rohan Das", "ECE", 7.2, List.of("C++", "Python", "DSA")));
        service.registerStudent(new Student("S104", "Meera Iyer", "CSE", 6.4, List.of("Java", "HTML", "CSS")));
        service.registerCompany(Company.of("C201", "NovaTech", "Backend Engineer", 7.5,
                List.of("CSE", "ISE"), List.of("Java", "SQL")));
        service.registerCompany(Company.of("C202", "DataVista", "Data Analyst", 7.0,
                List.of("CSE", "ISE", "ECE"), List.of("Python")));
        System.out.println("Loaded 4 students and 2 companies.");
    }

    private void registerStudent() {
        Student student = new Student(
                Input.required(scanner, "Student ID: "),
                Input.required(scanner, "Name: "),
                Input.required(scanner, "Branch: "),
                Input.cgpa(scanner, "CGPA (0-10): "),
                Input.commaSeparated(scanner, "Skills (comma-separated; may be empty): "));
        service.registerStudent(student);
        System.out.println("Student registered: " + student.id());
    }

    private void registerCompany() {
        Company company = Company.of(
                Input.required(scanner, "Company ID: "),
                Input.required(scanner, "Company name: "),
                Input.required(scanner, "Job role: "),
                Input.cgpa(scanner, "Minimum CGPA (0-10): "),
                Input.commaSeparated(scanner, "Eligible branches (comma-separated): "),
                Input.commaSeparated(scanner, "Required skills (comma-separated): "));
        service.registerCompany(company);
        System.out.println("Company registered: " + company.id());
    }

    private void listStudents() { printRows("Students", service.allStudents()); }
    private void listCompanies() { printRows("Companies", service.allCompanies()); }

    private void showEligibleStudents() {
        String companyId = Input.required(scanner, "Company ID: ");
        printRows("Eligible students", service.getEligibleStudents(companyId));
    }

    private void apply() {
        PlacementApplication application = service.apply(
                Input.required(scanner, "Student ID: "), Input.required(scanner, "Company ID: "));
        System.out.println("Application submitted: " + application);
    }

    private void shortlist() {
        String studentId = Input.required(scanner, "Student ID: ");
        String companyId = Input.required(scanner, "Company ID: ");
        service.shortlist(studentId, companyId);
        System.out.println(studentId + " has been shortlisted for " + companyId + ".");
    }

    private void select() {
        String studentId = Input.required(scanner, "Student ID: ");
        String companyId = Input.required(scanner, "Company ID: ");
        service.select(studentId, companyId);
        System.out.println(studentId + " has been selected by " + companyId + ".");
    }

    private void showStudentApplications() {
        String studentId = Input.required(scanner, "Student ID: ");
        printRows("Applications", service.getApplicationsForStudent(studentId));
    }

    private void searchStudents() {
        System.out.print("Search by name, ID, or branch: ");
        printRows("Search results", service.searchStudents(scanner.nextLine()));
    }

    private void runConcurrentReport() {
        String companyId = Input.required(scanner, "Company ID for eligibility report: ");
        EligibilityResult eligibility = service.generateEligibilityAsync(companyId).join();
        PlacementReport report = service.generateReportAsync().join();
        printRows("Eligible students for " + eligibility.companyId(), eligibility.eligibleStudents());
        System.out.printf("Report: %d students, %d companies, %d applications, placement statuses %s%n",
                report.studentCount(), report.companyCount(), report.applicationCount(), report.studentsByStatus());
    }

    private static void printRows(String title, List<?> rows) {
        System.out.println("\n" + title + " (" + rows.size() + "):");
        if (rows.isEmpty()) System.out.println("  No records found.");
        else rows.forEach(row -> System.out.println("  " + row));
    }
}
