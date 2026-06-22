package com.project.klare_server.wallet.dto;

import com.project.klare_server.wallet.domain.FundingStatus;
import com.project.klare_server.wallet.domain.WalletFunding;
import java.math.BigDecimal;

public record FundingResponse(
        String externalRef,
        FundingStatus status,
        BigDecimal amount,
        String payer,
        boolean otpRequired,
        String message) {

    public static FundingResponse from(WalletFunding funding) {
        return new FundingResponse(
                funding.getExternalRef(),
                funding.getStatus(),
                funding.getAmount(),
                funding.getPayer(),
                funding.getStatus() == FundingStatus.AWAITING_OTP,
                funding.getMessage());
    }
}
