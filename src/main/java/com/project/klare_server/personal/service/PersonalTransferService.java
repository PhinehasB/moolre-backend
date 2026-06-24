package com.project.klare_server.personal.service;

import com.project.klare_server.common.error.ApiException;
import com.project.klare_server.common.error.ErrorCode;
import com.project.klare_server.moolre.GhanaMobileMoney;
import com.project.klare_server.moolre.MoolreClient;
import com.project.klare_server.moolre.MoolreException;
import com.project.klare_server.personal.domain.PersonalTransaction;
import com.project.klare_server.personal.domain.PersonalWallet;
import com.project.klare_server.personal.domain.TransactionStatus;
import com.project.klare_server.personal.domain.TransactionType;
import com.project.klare_server.personal.dto.TransferRequest;
import com.project.klare_server.personal.dto.TransferResponse;
import com.project.klare_server.personal.repository.PersonalTransactionRepository;
import com.project.klare_server.personal.repository.PersonalWalletRepository;
import com.project.klare_server.personal.security.AuthenticatedPersonalUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PersonalTransferService {

    private static final BigDecimal KLARE_FEE_RATE = new BigDecimal("0.005");

    private final PersonalWalletRepository walletRepository;
    private final PersonalTransactionRepository transactionRepository;
    private final MoolreClient moolreClient;

    public PersonalTransferService(
            PersonalWalletRepository walletRepository,
            PersonalTransactionRepository transactionRepository,
            MoolreClient moolreClient) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.moolreClient = moolreClient;
    }

    @Transactional
    public TransferResponse send(AuthenticatedPersonalUser principal, TransferRequest request) {
        BigDecimal amount = scale(request.amount());
        BigDecimal klareFee = scale(amount.multiply(KLARE_FEE_RATE));
        BigDecimal total = scale(amount.add(klareFee));

        if ("BANK".equalsIgnoreCase(request.network().trim())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "Bank transfers are coming soon. Please choose MTN, Telecel, or AT for now.");
        }

        String channel;
        String receiver;
        try {
            channel = GhanaMobileMoney.resolveChannel(request.phone());
            receiver = GhanaMobileMoney.normalize(request.phone());
        } catch (MoolreException ex) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "That doesn't look like a valid Ghana mobile money number.");
        }

        PersonalWallet wallet = walletRepository
                .findByAccountIdAndAccountType(principal.id(), principal.accountType())
                .orElseGet(() -> createWallet(principal));

        if (wallet.getFreeBalance().compareTo(total) < 0) {
            throw new ApiException(ErrorCode.INSUFFICIENT_FUNDS,
                    "Not enough spendable cash. You have " + wallet.getCurrency() + " "
                            + wallet.getFreeBalance().toPlainString() + " but this transfer costs "
                            + wallet.getCurrency() + " " + total.toPlainString() + ".");
        }

        String externalRef = "PSND-" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);

        MoolreClient.TransferResult result;
        try {
            result = moolreClient.transfer(channel, amount, receiver, externalRef, "Klare transfer");
        } catch (MoolreException ex) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "We couldn't reach the payment network. No money has left your wallet — please try again.");
        }

        Integer tx = result.txstatus();
        TransactionStatus status;
        if (tx != null && tx == 1) {
            status = TransactionStatus.COMPLETED;
        } else if (tx != null && (tx == 0 || tx == 3)) {
            status = TransactionStatus.PENDING;
        } else {
            String reason = StringUtils.hasText(result.message()) ? result.message()
                    : "The transfer could not be completed. No money has left your wallet.";
            throw new ApiException(ErrorCode.VALIDATION_ERROR, reason);
        }

        wallet.setFreeBalance(scale(wallet.getFreeBalance().subtract(total)));

        String displayName = StringUtils.hasText(result.receiverName()) ? result.receiverName() : receiver;
        String reference = StringUtils.hasText(result.transactionId()) ? result.transactionId() : externalRef;

        PersonalTransaction txn = new PersonalTransaction();
        txn.setAccountId(principal.id());
        txn.setAccountType(principal.accountType());
        txn.setType(TransactionType.TRANSFER);
        txn.setAmount(amount);
        txn.setKlareFee(klareFee);
        txn.setTotal(total);
        txn.setNetwork(request.network().trim());
        txn.setRecipient(receiver);
        txn.setRecipientName(result.receiverName());
        txn.setExternalRef(externalRef);
        txn.setTransactionId(result.transactionId());
        txn.setDescription("Sent to " + receiver + " on " + request.network().trim());
        txn.setTitle("Sent to " + displayName);
        txn.setSubtitle(request.network().trim() + " · " + reference);
        txn.setStatus(status);
        transactionRepository.save(txn);

        return TransferResponse.from(txn, wallet.getCurrency(), wallet.getFreeBalance());
    }

    private PersonalWallet createWallet(AuthenticatedPersonalUser principal) {
        PersonalWallet wallet = new PersonalWallet();
        wallet.setAccountId(principal.id());
        wallet.setAccountType(principal.accountType());
        return walletRepository.save(wallet);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
