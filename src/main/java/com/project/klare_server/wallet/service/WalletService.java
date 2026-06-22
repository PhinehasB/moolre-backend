package com.project.klare_server.wallet.service;

import com.project.klare_server.auth.domain.BusinessUser;
import com.project.klare_server.auth.repository.BusinessUserRepository;
import com.project.klare_server.common.error.ApiException;
import com.project.klare_server.common.error.ErrorCode;
import com.project.klare_server.common.error.ResourceNotFoundException;
import com.project.klare_server.common.error.UnauthorizedException;
import com.project.klare_server.company.domain.Company;
import com.project.klare_server.company.domain.CompanyWallet;
import com.project.klare_server.company.repository.CompanyRepository;
import com.project.klare_server.company.repository.CompanyWalletRepository;
import com.project.klare_server.moolre.GhanaMobileMoney;
import com.project.klare_server.moolre.MoolreClient;
import com.project.klare_server.moolre.MoolreException;
import com.project.klare_server.payroll.domain.PayrollRun;
import com.project.klare_server.payroll.domain.PayrollRunStatus;
import com.project.klare_server.payroll.repository.PayrollRunRepository;
import com.project.klare_server.wallet.domain.FundingStatus;
import com.project.klare_server.wallet.domain.WalletFunding;
import com.project.klare_server.wallet.dto.FundWalletRequest;
import com.project.klare_server.wallet.dto.FundingResponse;
import com.project.klare_server.wallet.dto.LedgerEntry;
import com.project.klare_server.wallet.dto.WalletResponse;
import com.project.klare_server.wallet.repository.WalletFundingRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);
    private static final String OTP_REQUIRED_CODE = "TP14";
    private static final int LEDGER_SIZE = 10;

    private final WalletFundingRepository fundingRepository;
    private final CompanyWalletRepository walletRepository;
    private final CompanyRepository companyRepository;
    private final BusinessUserRepository businessUserRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final MoolreClient moolreClient;

    public WalletService(
            WalletFundingRepository fundingRepository,
            CompanyWalletRepository walletRepository,
            CompanyRepository companyRepository,
            BusinessUserRepository businessUserRepository,
            PayrollRunRepository payrollRunRepository,
            MoolreClient moolreClient) {
        this.fundingRepository = fundingRepository;
        this.walletRepository = walletRepository;
        this.companyRepository = companyRepository;
        this.businessUserRepository = businessUserRepository;
        this.payrollRunRepository = payrollRunRepository;
        this.moolreClient = moolreClient;
    }

    @Transactional
    public WalletResponse getWallet(UUID userId) {
        BusinessUser user = businessUserRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
        Company company = user.getCompany();
        CompanyWallet wallet = walletRepository.findByCompanyId(company.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        provisionBankAccount(wallet, user);

        WalletResponse.BankTopUp bankTopUp = StringUtils.hasText(wallet.getBankAccountNo())
                ? new WalletResponse.BankTopUp(wallet.getBankAccountName(), wallet.getBankAccountNo(), wallet.getBankName())
                : null;

        return new WalletResponse(
                wallet.getBalance(),
                wallet.getPending(),
                wallet.getCurrency(),
                company.getName(),
                maskAccount(wallet.getBankAccountNo()),
                bankTopUp,
                buildLedger(company.getId()));
    }

    private void provisionBankAccount(CompanyWallet wallet, BusinessUser user) {
        if (StringUtils.hasText(wallet.getBankAccountNo())) {
            return;
        }
        try {
            MoolreClient.BankAccount account = moolreClient.createBankAccount(
                    user.getFirstName(),
                    user.getLastName(),
                    GhanaMobileMoney.toInternational(user.getPhone()),
                    user.getEmail(),
                    "klr" + UUID.randomUUID().toString().replace("-", ""));
            wallet.setBankAccountNo(account.accountNo());
            wallet.setBankAccountName(account.accountName());
            wallet.setBankName(account.bankName());
        } catch (MoolreException ex) {
            log.warn("Could not provision bank account for company={} error={}", wallet.getCompany().getId(), ex.getMessage());
        }
    }

    private List<LedgerEntry> buildLedger(UUID companyId) {
        List<LedgerEntry> entries = new ArrayList<>();
        for (WalletFunding funding : fundingRepository.findTop10ByCompanyIdOrderByCreatedAtDesc(companyId)) {
            String method = "BANK".equals(funding.getSource()) ? "Bank transfer" : "MoMo";
            entries.add(new LedgerEntry(
                    funding.getCreatedAt(),
                    "Wallet top-up · " + method,
                    reference(funding.getTransactionId() != null ? funding.getTransactionId() : funding.getExternalRef()),
                    fundingStatusLabel(funding.getStatus()),
                    "CREDIT",
                    funding.getAmount()));
        }
        for (PayrollRun run : payrollRunRepository.findTop20ByCompanyIdOrderByCreatedAtDesc(companyId)) {
            if (run.getStatus() == PayrollRunStatus.COMPLETED
                    || run.getStatus() == PayrollRunStatus.PROCESSING
                    || run.getStatus() == PayrollRunStatus.FAILED) {
                entries.add(new LedgerEntry(
                        run.getCompletedAt() != null ? run.getCompletedAt() : run.getCreatedAt(),
                        "Payroll run · " + run.getEmployeeCount() + " employees",
                        reference(run.getId().toString()),
                        runStatusLabel(run.getStatus()),
                        "DEBIT",
                        run.getTotalAmount()));
            }
        }
        return entries.stream()
                .sorted(Comparator.comparing(LedgerEntry::date).reversed())
                .limit(LEDGER_SIZE)
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
            return "MLR-" + Integer.toHexString(System.identityHashCode(this)).toUpperCase();
        }
        String cleaned = source.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return "MLR-" + cleaned.substring(0, Math.min(7, cleaned.length()));
    }

    private String maskAccount(String accountNo) {
        if (!StringUtils.hasText(accountNo)) {
            return null;
        }
        return "****" + accountNo.substring(Math.max(0, accountNo.length() - 4));
    }

    @Transactional
    public FundingResponse fund(UUID companyId, FundWalletRequest request) {
        String payer = GhanaMobileMoney.normalize(request.payer());
        String channel = GhanaMobileMoney.resolvePaymentChannel(request.payer());
        String externalRef = "kf_" + UUID.randomUUID().toString().replace("-", "");

        WalletFunding funding = new WalletFunding();
        funding.setCompany(companyRepository.getReferenceById(companyId));
        funding.setExternalRef(externalRef);
        funding.setPayer(payer);
        funding.setChannel(channel);
        funding.setAmount(request.amount());
        funding.setStatus(FundingStatus.AWAITING_APPROVAL);

        MoolreClient.PaymentResult result = moolreClient.initiatePayment(
                channel, payer, request.amount(), externalRef, null, null, "Klare wallet top up");
        applyPaymentResult(funding, result);

        fundingRepository.save(funding);
        return FundingResponse.from(funding);
    }

    @Transactional
    public FundingResponse submitOtp(UUID companyId, String externalRef, String otpcode) {
        WalletFunding funding = fundingRepository.findByExternalRefAndCompanyId(externalRef, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Funding request not found"));
        if (funding.getStatus() != FundingStatus.AWAITING_OTP) {
            throw new ApiException(ErrorCode.CONFLICT, "This funding request is not awaiting an OTP");
        }

        MoolreClient.PaymentResult verification = moolreClient.initiatePayment(
                funding.getChannel(), funding.getPayer(), funding.getAmount(), externalRef, otpcode, null, "Klare wallet top up");
        if (OTP_REQUIRED_CODE.equalsIgnoreCase(verification.code())) {
            funding.setMessage(verification.message() != null ? verification.message() : "Incorrect or expired code, try again");
            return FundingResponse.from(funding);
        }
        if (verification.status() != 1) {
            funding.setStatus(FundingStatus.FAILED);
            funding.setMessage(verification.message() != null ? verification.message() : "Verification failed");
            return FundingResponse.from(funding);
        }

        MoolreClient.PaymentResult charge = moolreClient.initiatePayment(
                funding.getChannel(), funding.getPayer(), funding.getAmount(), externalRef, null, null, "Klare wallet top up");
        applyPaymentResult(funding, charge);
        return FundingResponse.from(funding);
    }

    @Transactional
    public FundingResponse checkStatus(UUID companyId, String externalRef) {
        WalletFunding funding = fundingRepository.findByExternalRefAndCompanyId(externalRef, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Funding request not found"));
        if (funding.getStatus() == FundingStatus.SUCCESS || funding.getStatus() == FundingStatus.FAILED) {
            return FundingResponse.from(funding);
        }
        try {
            MoolreClient.TransferStatusResult status = moolreClient.transferStatus(externalRef);
            Integer tx = status.txstatus();
            if (tx != null && tx == 1) {
                funding.setTransactionId(status.transactionId());
                markSuccessAndCredit(funding);
            } else if (tx != null && tx == 2) {
                funding.setStatus(FundingStatus.FAILED);
                funding.setMessage("Payment failed");
            }
        } catch (MoolreException ignored) {
            // leave unchanged; the caller can poll again
        }
        return FundingResponse.from(funding);
    }

    @Transactional
    public void creditFromWebhook(String externalRef, String transactionId, int txstatus, String accountNumber, BigDecimal amount) {
        if (StringUtils.hasText(externalRef)) {
            WalletFunding funding = fundingRepository.findByExternalRef(externalRef).orElse(null);
            if (funding != null) {
                if (txstatus == 1) {
                    funding.setTransactionId(transactionId);
                    markSuccessAndCredit(funding);
                } else if (txstatus == 2) {
                    funding.setStatus(FundingStatus.FAILED);
                    funding.setMessage("Payment failed");
                }
                return;
            }
        }
        if (txstatus == 1 && StringUtils.hasText(accountNumber) && amount != null && amount.signum() > 0) {
            walletRepository.findByBankAccountNo(accountNumber).ifPresent(wallet -> {
                WalletFunding bank = new WalletFunding();
                bank.setCompany(wallet.getCompany());
                bank.setExternalRef(StringUtils.hasText(externalRef)
                        ? externalRef : "bank_" + UUID.randomUUID().toString().replace("-", ""));
                bank.setPayer("");
                bank.setChannel("BANK");
                bank.setSource("BANK");
                bank.setAmount(amount);
                bank.setStatus(FundingStatus.SUCCESS);
                bank.setTransactionId(transactionId);
                bank.setCredited(true);
                bank.setMessage("Bank transfer received");
                wallet.setBalance(wallet.getBalance().add(amount));
                fundingRepository.save(bank);
            });
        }
    }

    private void applyPaymentResult(WalletFunding funding, MoolreClient.PaymentResult result) {
        if (OTP_REQUIRED_CODE.equalsIgnoreCase(result.code())) {
            funding.setStatus(FundingStatus.AWAITING_OTP);
            funding.setMessage(result.message() != null ? result.message() : "Enter the OTP sent to your phone");
            return;
        }
        if (result.status() == 1) {
            funding.setStatus(FundingStatus.AWAITING_APPROVAL);
            funding.setTransactionId(result.data());
            funding.setMessage(result.message() != null ? result.message() : "Approve the prompt on your phone");
            return;
        }
        funding.setStatus(FundingStatus.FAILED);
        funding.setMessage(result.message() != null ? result.message() : "Payment could not be initiated");
    }

    private void markSuccessAndCredit(WalletFunding funding) {
        funding.setStatus(FundingStatus.SUCCESS);
        funding.setMessage("Wallet topped up");
        if (!funding.isCredited()) {
            CompanyWallet wallet = walletRepository.findByCompanyId(funding.getCompany().getId())
                    .orElseThrow(() -> new ApiException(ErrorCode.CONFLICT, "Wallet not found"));
            wallet.setBalance(wallet.getBalance().add(funding.getAmount()));
            funding.setCredited(true);
        }
    }
}
