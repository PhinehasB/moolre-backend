package com.project.klare_server.common.error;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
