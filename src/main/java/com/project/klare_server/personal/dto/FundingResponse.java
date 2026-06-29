package com.project.klare_server.personal.dto;

import com.project.klare_server.personal.domain.PersonalFunding;
import java.math.BigDecimal;

public record FundingResponse(
        String externalRef,
        String status,
        String message,
        BigDecimal amount) {

    public static FundingResponse from(PersonalFunding funding) {
        return new FundingResponse(
                funding.getExternalRef(),
                funding.getStatus().name(),
                funding.getMessage(),
                funding.getAmount());
    }
}
