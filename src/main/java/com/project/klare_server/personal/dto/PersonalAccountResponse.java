package com.project.klare_server.personal.dto;

import com.project.klare_server.employee.domain.Employee;
import com.project.klare_server.personal.domain.PersonalAccountType;
import com.project.klare_server.personal.domain.PersonalUser;
import java.util.UUID;

public record PersonalAccountResponse(
        UUID id,
        PersonalAccountType accountType,
        String firstName,
        String lastName,
        String username,
        String email,
        String phone,
        String role,
        String companyName,
        String status,
        boolean mustChangePassword) {

    public static PersonalAccountResponse fromEmployee(Employee employee) {
        return new PersonalAccountResponse(
                employee.getId(),
                PersonalAccountType.EMPLOYEE,
                employee.getFirstName(),
                employee.getLastName(),
                employee.getUsername(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getRole(),
                employee.getCompany().getName(),
                employee.getStatus().name(),
                employee.isMustChangePassword());
    }

    public static PersonalAccountResponse fromIndividual(PersonalUser user) {
        return new PersonalAccountResponse(
                user.getId(),
                PersonalAccountType.INDIVIDUAL,
                user.getFirstName(),
                user.getLastName(),
                null,
                user.getEmail(),
                user.getPhone(),
                null,
                null,
                user.getStatus().name(),
                false);
    }
}
