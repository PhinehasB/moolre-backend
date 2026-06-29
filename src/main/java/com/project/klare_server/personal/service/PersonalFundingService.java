package com.project.klare_server.personal.service;

import com.project.klare_server.common.error.ApiException;
import com.project.klare_server.common.error.ErrorCode;
import com.project.klare_server.common.error.ResourceNotFoundException;
import com.project.klare_server.moolre.GhanaMobileMoney;
import com.project.klare_server.moolre.MoolreClient;
import com.project.klare_server.moolre.MoolreException;
import com.project.klare_server.personal.domain.PersonalFunding;
import com.project.klare_server.personal.domain.PersonalTransaction;
import com.project.klare_server.personal.domain.PersonalWallet;
import com.project.klare_server.personal.domain.TransactionStatus;
import com.project.klare_server.personal.domain.TransactionType;
import com.project.klare_server.personal.dto.FundWalletRequest;
import com.project.klare_server.personal.dto.FundingResponse;
import com.project.klare_server.personal.repository.PersonalFundingRepository;
import com.project.klare_server.personal.repository.PersonalTransactionRepository;
import com.project.klare_server.personal.repository.PersonalWalletRepository;
import com.project.klare_server.personal.security.AuthenticatedPersonalUser;
import com.project.klare_server.wallet.domain.FundingStatus;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalFundingService {

    private static final String OTP_REQUIRED_CODE = "TP14";

    private final PersonalFundingRepository fundingRepository;
    private final PersonalWalletRepository walletRepository;
    private final PersonalTransactionRepository transactionRepository;
    private final MoolreClient moolreClient;

    public PersonalFundingService(
            PersonalFundingRepository fundingRepository,
            PersonalWalletRepository walletRepository,
            PersonalTransactionRepository transactionRepository,
            MoolreClient moolreClient) {
        this.fundingRepository = fundingRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.moolreClient = moolreClient;
    }

    @Transactional
    public FundingResponse fund(AuthenticatedPersonalUser principal, FundWalletRequest request) {
        String payer = GhanaMobileMoney.normalize(request.payer());
        String channel;
        try {
            channel = GhanaMobileMoney.resolvePaymentChannel(request.payer());
        } catch (MoolreException ex) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "That doesn't look like a valid Ghana mobile money number.");
        }

        String externalRef = "pf_" + UUID.randomUUID().toString().replace("-", "");
        PersonalFunding funding = new PersonalFunding();
        funding.setAccountId(principal.id());
        funding.setAccountType(principal.accountType());
        funding.setExternalRef(externalRef);
        funding.setPayer(payer);
        funding.setChannel(channel);
        funding.setAmount(request.amount());
        funding.setStatus(FundingStatus.AWAITING_APPROVAL);

        try {
            MoolreClient.PaymentResult result = moolreClient.initiatePayment(
                    channel, payer, request.amount(), externalRef, null, null, "Klare wallet top up");
            applyPaymentResult(funding, result);
        } catch (MoolreException ex) {
            funding.setStatus(FundingStatus.FAILED);
            funding.setMessage("We couldn't reach the payment network. Please try again.");
        }

        fundingRepository.save(funding);
        return FundingResponse.from(funding);
    }

    @Transactional
    public FundingResponse submitOtp(AuthenticatedPersonalUser principal, String externalRef, String otpcode) {
        PersonalFunding funding = load(principal, externalRef);
        if (funding.getStatus() != FundingStatus.AWAITING_OTP) {
            throw new ApiException(ErrorCode.CONFLICT, "This top-up is not awaiting an OTP");
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
    public FundingResponse checkStatus(AuthenticatedPersonalUser principal, String externalRef) {
        PersonalFunding funding = load(principal, externalRef);
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
            funding.setMessage(funding.getMessage());
        }
        return FundingResponse.from(funding);
    }

    private PersonalFunding load(AuthenticatedPersonalUser principal, String externalRef) {
        return fundingRepository
                .findByExternalRefAndAccountIdAndAccountType(externalRef, principal.id(), principal.accountType())
                .orElseThrow(() -> new ResourceNotFoundException("Top-up request not found"));
    }

    private void applyPaymentResult(PersonalFunding funding, MoolreClient.PaymentResult result) {
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

    private void markSuccessAndCredit(PersonalFunding funding) {
        funding.setStatus(FundingStatus.SUCCESS);
        funding.setMessage("Wallet topped up");
        if (!funding.isCredited()) {
            PersonalWallet wallet = walletRepository
                    .findByAccountIdAndAccountType(funding.getAccountId(), funding.getAccountType())
                    .orElseGet(() -> createWallet(funding));
            wallet.setFreeBalance(wallet.getFreeBalance().add(funding.getAmount()));
            funding.setCredited(true);
            transactionRepository.save(fundTransaction(funding));
        }
    }

    private PersonalTransaction fundTransaction(PersonalFunding funding) {
        PersonalTransaction txn = new PersonalTransaction();
        txn.setAccountId(funding.getAccountId());
        txn.setAccountType(funding.getAccountType());
        txn.setType(TransactionType.FUND);
        txn.setAmount(funding.getAmount());
        txn.setTotal(funding.getAmount());
        txn.setRecipient(funding.getPayer());
        txn.setTitle("Wallet top-up");
        txn.setSubtitle("From " + funding.getPayer());
        txn.setDescription("Mobile money top-up");
        txn.setExternalRef(funding.getExternalRef());
        txn.setTransactionId(funding.getTransactionId());
        txn.setStatus(TransactionStatus.COMPLETED);
        return txn;
    }

    private PersonalWallet createWallet(PersonalFunding funding) {
        PersonalWallet wallet = new PersonalWallet();
        wallet.setAccountId(funding.getAccountId());
        wallet.setAccountType(funding.getAccountType());
        return walletRepository.save(wallet);
    }
}
