package com.project.klare_server.personal.service;

import com.project.klare_server.moolre.GhanaMobileMoney;
import com.project.klare_server.moolre.MoolreClient;
import com.project.klare_server.moolre.MoolreException;
import com.project.klare_server.personal.domain.PersonalObligation;
import com.project.klare_server.personal.domain.PersonalTransaction;
import com.project.klare_server.personal.domain.PersonalWallet;
import com.project.klare_server.personal.domain.TransactionStatus;
import com.project.klare_server.personal.domain.TransactionType;
import com.project.klare_server.personal.dto.BillPaymentResponse;
import com.project.klare_server.personal.repository.PersonalObligationRepository;
import com.project.klare_server.personal.repository.PersonalTransactionRepository;
import com.project.klare_server.personal.repository.PersonalWalletRepository;
import com.project.klare_server.personal.security.AuthenticatedPersonalUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PersonalBillPaymentService {

    private static final Logger log = LoggerFactory.getLogger(PersonalBillPaymentService.class);

    private final PersonalObligationRepository obligationRepository;
    private final PersonalWalletRepository walletRepository;
    private final PersonalTransactionRepository transactionRepository;
    private final MoolreClient moolreClient;

    public PersonalBillPaymentService(
            PersonalObligationRepository obligationRepository,
            PersonalWalletRepository walletRepository,
            PersonalTransactionRepository transactionRepository,
            MoolreClient moolreClient) {
        this.obligationRepository = obligationRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.moolreClient = moolreClient;
    }

    @Transactional
    public BillPaymentResponse payActiveBills(AuthenticatedPersonalUser principal) {
        List<PersonalObligation> active = obligationRepository
                .findByAccountIdAndAccountTypeAndActiveTrueOrderByCreatedAtAsc(principal.id(), principal.accountType());
        PersonalWallet wallet = walletRepository
                .findByAccountIdAndAccountType(principal.id(), principal.accountType())
                .orElseGet(() -> createWallet(principal));

        int paid = 0;
        int skipped = 0;
        BigDecimal totalPaid = BigDecimal.ZERO;

        for (PersonalObligation bill : active) {
            BigDecimal amount = scale(bill.getAmount());
            if (!StringUtils.hasText(bill.getRecipientNumber())
                    || wallet.getLockedBalance().compareTo(amount) < 0) {
                skipped++;
                continue;
            }
            String channel;
            String receiver;
            try {
                channel = GhanaMobileMoney.resolveChannel(bill.getRecipientNumber());
                receiver = GhanaMobileMoney.normalize(bill.getRecipientNumber());
            } catch (MoolreException ex) {
                skipped++;
                continue;
            }

            String externalRef = "PBILL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            try {
                MoolreClient.TransferResult result = moolreClient.transfer(channel, amount, receiver, externalRef, "Klare bill: " + bill.getName());
                Integer tx = result.txstatus();
                if (tx == null || !(tx == 1 || tx == 0 || tx == 3)) {
                    skipped++;
                    continue;
                }
                wallet.setLockedBalance(scale(wallet.getLockedBalance().subtract(amount)));
                transactionRepository.save(billTransaction(principal.id(), bill, amount,
                        StringUtils.hasText(result.transactionId()) ? result.transactionId() : externalRef));
                paid++;
                totalPaid = totalPaid.add(amount);
            } catch (MoolreException ex) {
                log.warn("Bill payment failed for {}: {}", bill.getName(), ex.getMessage());
                skipped++;
            }
        }

        return new BillPaymentResponse(
                paid, skipped, scale(totalPaid), wallet.getCurrency(),
                wallet.getFreeBalance(), wallet.getLockedBalance());
    }

    private PersonalTransaction billTransaction(UUID accountId, PersonalObligation bill, BigDecimal amount, String reference) {
        PersonalTransaction txn = new PersonalTransaction();
        txn.setAccountId(accountId);
        txn.setAccountType(bill.getAccountType());
        txn.setType(TransactionType.TRANSFER);
        txn.setAmount(amount);
        txn.setTotal(amount);
        txn.setNetwork(bill.getNetwork());
        txn.setRecipient(bill.getRecipientNumber());
        txn.setTitle("Paid " + bill.getName());
        txn.setSubtitle((bill.getNetwork() != null ? bill.getNetwork() + " · " : "") + reference);
        txn.setDescription("Bill payment: " + bill.getName());
        txn.setStatus(TransactionStatus.COMPLETED);
        return txn;
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
