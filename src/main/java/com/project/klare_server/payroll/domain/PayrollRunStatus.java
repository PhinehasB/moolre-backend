package com.project.klare_server.payroll.domain;

public enum PayrollRunStatus {
    PENDING_CONFIRMATION,
    PROCESSING,
    COMPLETED,
    CANCELLED,
    EXPIRED,
    FAILED
}
