# Smart Student Placement Management System

A Java 17 console application that models the major workflows of a college placement cell:

- student and company registration with validation
- eligibility verification by CGPA, branch, and required skills
- applications with duplicate prevention
- shortlisting, selection, and placement-status updates
- stream-based searches and reports
- concurrent eligibility/report generation using `CompletableFuture`
- unit tests for core business rules

## Run

Install JDK 17+ and Maven, then from this directory run:

```bash
mvn test
mvn exec:java
```

Choose **1** in the menu to load a small demonstration dataset, or use the remaining menu options to manage records interactively.

For a one-file online-compiler demonstration, use `OnlinePlacementDemo.java`. It requires no Maven setup and runs automatically with sample data.

## Run online with GitHub Codespaces

This repository includes `.devcontainer/devcontainer.json`, which configures Java 17 and Maven automatically. After uploading this project to GitHub, choose **Code -> Codespaces -> Create codespace on main**. When the browser workspace is ready, run:

```bash
mvn test
mvn exec:java
```

## Design notes

`PlacementService` owns the in-memory repositories and enforces business rules. Domain classes represent students, companies, and applications. Thread-safe maps and atomic application creation make the example safe when eligibility/report jobs run independently from normal placement operations.
