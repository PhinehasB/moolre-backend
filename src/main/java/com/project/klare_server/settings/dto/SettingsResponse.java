package com.project.klare_server.settings.dto;

public record SettingsResponse(PayrollAutomation payrollAutomation, CompanyProfile companyProfile) {

    public record PayrollAutomation(
            boolean automaticPayroll,
            int payDate,
            boolean emailEstimateBeforeRun,
            boolean notifyEmployeesBeforePayday) {
    }

    public record CompanyProfile(String companyName, String registrationNumber, String adminEmail) {
    }
}
