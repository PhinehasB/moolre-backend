package com.project.klare_server.wallet.dto;

import java.math.BigDecimal;
import java.util.List;

public record WalletResponse(
        BigDecimal balance,
        BigDecimal pending,
        String currency,
        String companyName,
        String settlementAccountMasked,
        BankTopUp bankTopUp,
        List<LedgerEntry> ledger) {

    public record BankTopUp(String accountName, String accountNumber, String bankName) {
    }
}
