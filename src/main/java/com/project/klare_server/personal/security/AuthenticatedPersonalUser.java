package com.project.klare_server.personal.security;

import com.project.klare_server.personal.domain.PersonalAccountType;
import java.util.UUID;

public record AuthenticatedPersonalUser(
        UUID id,
        PersonalAccountType accountType,
        UUID companyId,
        boolean mustChangePassword) {
}
