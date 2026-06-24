package com.project.klare_server.personal.security;

import com.project.klare_server.employee.repository.EmployeeRepository;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class CredentialGenerator {

    private static final String UPPER = "ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String ALL = UPPER + LOWER + DIGITS;

    private final EmployeeRepository employeeRepository;
    private final SecureRandom random = new SecureRandom();

    public CredentialGenerator(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public String uniqueUsername(String firstName, String companyName) {
        String base = capitalize(letters(firstName)) + capitalize(token(companyName));
        if (base.isBlank()) {
            base = "Member";
        }
        String candidate = base;
        int suffix = 1;
        while (employeeRepository.existsByUsername(candidate)) {
            suffix++;
            candidate = base + suffix;
        }
        return candidate;
    }

    public String temporaryPassword() {
        StringBuilder builder = new StringBuilder();
        builder.append(pick(UPPER));
        builder.append(pick(LOWER));
        builder.append(pick(DIGITS));
        builder.append(pick(DIGITS));
        for (int i = 0; i < 6; i++) {
            builder.append(pick(ALL));
        }
        return shuffle(builder.toString());
    }

    private String letters(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z]", "");
    }

    private String token(String companyName) {
        String firstWord = companyName == null ? "" : companyName.trim().split("\\s+")[0];
        String clean = letters(firstWord);
        return clean.length() > 5 ? clean.substring(0, 5) : clean;
    }

    private String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }

    private char pick(String pool) {
        return pool.charAt(random.nextInt(pool.length()));
    }

    private String shuffle(String value) {
        char[] chars = value.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}
