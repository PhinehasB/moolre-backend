package com.project.klare_server.employee.dto;

import com.project.klare_server.employee.domain.Employee;
import com.project.klare_server.employee.domain.EmployeeStatus;
import com.project.klare_server.employee.domain.WalletLinkStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String role,
        BigDecimal monthlySalary,
        EmployeeStatus status,
        WalletLinkStatus walletStatus,
        Instant createdAt) {

    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getRole(),
                employee.getMonthlySalary(),
                employee.getStatus(),
                employee.getWalletStatus(),
                employee.getCreatedAt());
    }
}
