package com.project.klare_server.moolre;

public class MoolreException extends RuntimeException {

    private final String code;

    public MoolreException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
