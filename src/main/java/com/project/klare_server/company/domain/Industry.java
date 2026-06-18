package com.project.klare_server.company.domain;

public enum Industry {
    TECHNOLOGY("Technology"),
    RETAIL_AND_COMMERCE("Retail & commerce"),
    FINANCIAL_SERVICES("Financial services"),
    MANUFACTURING("Manufacturing"),
    HOSPITALITY("Hospitality"),
    OTHER("Other");

    private final String label;

    Industry(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
