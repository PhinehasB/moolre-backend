package com.project.klare_server.payroll.service;

import com.project.klare_server.payroll.domain.PayrollRun;
import com.project.klare_server.payroll.domain.PayrollRunStatus;
import com.project.klare_server.payroll.repository.PayrollRunRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayrollConfirmationAttemptService {

    private final PayrollRunRepository payrollRunRepository;

    public PayrollConfirmationAttemptService(PayrollRunRepository payrollRunRepository) {
        this.payrollRunRepository = payrollRunRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordFailedAttempt(UUID runId, int maxAttempts) {
        PayrollRun run = payrollRunRepository.findById(runId).orElse(null);
        if (run == null) {
            return false;
        }
        int attempts = run.getConfirmationAttempts() + 1;
        run.setConfirmationAttempts(attempts);
        if (attempts >= maxAttempts) {
            run.setStatus(PayrollRunStatus.CANCELLED);
            return true;
        }
        return false;
    }
}
