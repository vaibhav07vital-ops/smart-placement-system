package com.alliance.placement.util;

import com.alliance.placement.exception.ValidationException;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public final class Input {
    private Input() { }

    public static String required(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();
        if (value.isEmpty()) throw new ValidationException("A value is required.");
        return value;
    }

    public static double cgpa(Scanner scanner, String prompt) {
        String value = required(scanner, prompt);
        try { return Double.parseDouble(value); }
        catch (NumberFormatException exception) { throw new ValidationException("Enter a valid number, for example 7.5."); }
    }

    public static List<String> commaSeparated(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return Arrays.stream(scanner.nextLine().split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).toList();
    }
}
