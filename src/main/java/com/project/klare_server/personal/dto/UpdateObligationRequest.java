package com.project.klare_server.personal.dto;

import com.project.klare_server.personal.domain.ObligationCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateObligationRequest(
        @Size(max = 120) String name,
        @DecimalMin(value = "0.01", message = "Enter an amount") BigDecimal amount,
        Boolean active,
        ObligationCategory category,
        @Size(max = 40) String network,
        @Size(max = 60) String recipientNumber) {
}
