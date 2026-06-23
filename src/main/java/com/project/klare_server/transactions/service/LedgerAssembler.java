package com.project.klare_server.transactions.service;

import com.project.klare_server.payroll.domain.PayrollRun;
import com.project.klare_server.payroll.domain.PayrollRunStatus;
import com.project.klare_server.payroll.repository.PayrollRunRepository;
import com.project.klare_server.wallet.domain.FundingStatus;
import com.project.klare_server.wallet.domain.WalletFunding;
import com.project.klare_server.wallet.dto.LedgerEntry;
import com.project.klare_server.wallet.repository.WalletFundingRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LedgerAssembler {

    private final WalletFundingRepository fundingRepository;
    private final PayrollRunRepository payrollRunRepository;

    public LedgerAssembler(WalletFundingRepository fundingRepository, PayrollRunRepository payrollRunRepository) {
        this.fundingRepository = fundingRepository;
        this.payrollRunRepository = payrollRunRepository;
    }

    public List<LedgerEntry> assemble(UUID companyId, boolean live) {
        List<LedgerEntry> entries = new ArrayList<>();

        for (WalletFunding funding : fundingRepository.findTop200ByCompanyIdAndLiveModeOrderByCreatedAtDesc(companyId, live)) {
            String method = "BANK".equals(funding.getSource()) ? "Bank transfer" : "MoMo";
            entries.add(new LedgerEntry(
                    funding.getCreatedAt(),
                    "Wallet top-up · " + method,
                    reference(funding.getTransactionId() != null ? funding.getTransactionId() : funding.getExternalRef()),
                    fundingStatusLabel(funding.getStatus()),
                    "CREDIT",
                    funding.getAmount()));
        }

        for (PayrollRun run : payrollRunRepository.findTop100ByCompanyIdAndLiveModeOrderByCreatedAtDesc(companyId, live)) {
            if (run.getStatus() != PayrollRunStatus.COMPLETED
                    && run.getStatus() != PayrollRunStatus.PROCESSING
                    && run.getStatus() != PayrollRunStatus.FAILED) {
                continue;
            }
            Instant date = run.getCompletedAt() != null ? run.getCompletedAt() : run.getCreatedAt();
            entries.add(new LedgerEntry(
                    date,
                    "Payroll run · " + run.getEmployeeCount() + " employees",
                    reference(run.getId().toString()),
                    runStatusLabel(run.getStatus()),
                    "DEBIT",
                    run.getTotalAmount()));
            if (run.getServiceFee() != null && run.getServiceFee().signum() > 0) {
                entries.add(new LedgerEntry(
                        date,
                        "Service fee · payroll",
                        reference(run.getId() + "F"),
                        "Success",
                        "DEBIT",
                        run.getServiceFee()));
            }
        }

        return entries.stream()
                .sorted(Comparator.comparing(LedgerEntry::date).reversed())
                .toList();
    }

    private String fundingStatusLabel(FundingStatus status) {
        return switch (status) {
            case SUCCESS -> "Success";
            case FAILED -> "Failed";
            default -> "Pending";
        };
    }

    private String runStatusLabel(PayrollRunStatus status) {
        return switch (status) {
            case COMPLETED -> "Success";
            case FAILED -> "Failed";
            default -> "Pending";
        };
    }

    private String reference(String source) {
        if (!StringUtils.hasText(source)) {
            return "MLR-000000";
        }
        String cleaned = source.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return "MLR-" + cleaned.substring(0, Math.min(7, cleaned.length()));
    }
}
