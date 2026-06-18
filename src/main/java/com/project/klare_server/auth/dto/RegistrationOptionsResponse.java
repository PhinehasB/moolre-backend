package com.project.klare_server.auth.dto;

import com.project.klare_server.company.domain.Industry;
import com.project.klare_server.company.domain.PayrollBand;
import java.util.Arrays;
import java.util.List;

public record RegistrationOptionsResponse(List<Option> industries, List<Option> payrollBands) {

    public record Option(String value, String label) {
    }

    public static RegistrationOptionsResponse build() {
        List<Option> industries = Arrays.stream(Industry.values())
                .map(industry -> new Option(industry.name(), industry.label()))
                .toList();
        List<Option> payrollBands = Arrays.stream(PayrollBand.values())
                .map(band -> new Option(band.name(), band.label()))
                .toList();
        return new RegistrationOptionsResponse(industries, payrollBands);
    }
}
