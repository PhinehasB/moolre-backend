package com.project.klare_server.payroll.domain;

import com.project.klare_server.common.persistence.BaseEntity;
import com.project.klare_server.employee.domain.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "payroll_items",
        indexes = @Index(name = "ix_payroll_items_run_id", columnList = "payroll_run_id"))
public class PayrollItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "employee_name", nullable = false, length = 201)
    private String employeeName;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayrollItemStatus status = PayrollItemStatus.PENDING;

    @Column(name = "external_ref", length = 64)
    private String externalRef;

    @Column(name = "transaction_id", length = 64)
    private String transactionId;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;
}
