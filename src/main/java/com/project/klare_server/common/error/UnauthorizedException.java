package com.project.klare_server.common.error;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
