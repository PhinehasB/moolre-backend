package com.project.klare_server.auth.dto;

import com.project.klare_server.company.domain.Company;
import com.project.klare_server.company.domain.CompanyStatus;
import com.project.klare_server.company.domain.Industry;
import com.project.klare_server.company.domain.PayrollBand;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        String registrationNumber,
        Industry industry,
        PayrollBand expectedMonthlyPayroll,
        CompanyStatus status) {

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(), company.getName(), company.getRegistrationNumber(),
                company.getIndustry(), company.getExpectedMonthlyPayroll(), company.getStatus());
    }
}
