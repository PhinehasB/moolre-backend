package com.project.klare_server.payroll.dto;

import com.project.klare_server.payroll.service.PayrollService;

public record AutoRunResponse(String outcome, String message, PayrollRunResponse run) {

    public static AutoRunResponse from(PayrollService.AutoRunResult result) {
        return new AutoRunResponse(result.outcome().name(), message(result.outcome()), result.run());
    }

    private static String message(PayrollService.AutoRunResult.Outcome outcome) {
        return switch (outcome) {
            case RAN -> "Payroll has been run and your team has been paid.";
            case AUTO_DISABLED -> "Automatic payroll is turned off. Enable it in Settings, or run payroll with the confirmation code.";
            case ALREADY_RUN -> "Payroll has already run for this month.";
            case NO_ACTIVE_EMPLOYEES -> "There are no active employees to pay.";
            case INSUFFICIENT_FUNDS -> "Your wallet does not cover this payroll. Please top up.";
        };
    }
}
