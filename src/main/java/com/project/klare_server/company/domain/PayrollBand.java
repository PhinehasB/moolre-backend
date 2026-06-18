package com.project.klare_server.company.domain;

public enum PayrollBand {
    UNDER_10K("Under GHS 10,000"),
    FROM_10K_TO_50K("GHS 10,000 – 50,000"),
    FROM_50K_TO_200K("GHS 50,000 – 200,000"),
    OVER_200K("Over GHS 200,000");

    private final String label;

    PayrollBand(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
