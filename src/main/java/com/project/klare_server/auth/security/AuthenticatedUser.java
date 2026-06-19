package com.project.klare_server.auth.security;

import com.project.klare_server.auth.domain.BusinessUserRole;
import java.util.UUID;

public record AuthenticatedUser(UUID id, UUID companyId, String email, BusinessUserRole role) {
}
