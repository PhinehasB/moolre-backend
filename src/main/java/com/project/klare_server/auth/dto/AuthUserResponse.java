package com.project.klare_server.auth.dto;

import com.project.klare_server.auth.domain.BusinessUser;
import com.project.klare_server.auth.domain.BusinessUserRole;
import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        BusinessUserRole role) {

    public static AuthUserResponse from(BusinessUser user) {
        return new AuthUserResponse(
                user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRole());
    }
}
