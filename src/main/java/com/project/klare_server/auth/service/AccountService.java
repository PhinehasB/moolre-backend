package com.project.klare_server.auth.service;

import com.project.klare_server.auth.domain.BusinessUser;
import com.project.klare_server.auth.dto.AuthUserResponse;
import com.project.klare_server.auth.dto.CompanyResponse;
import com.project.klare_server.auth.dto.MeResponse;
import com.project.klare_server.auth.repository.BusinessUserRepository;
import com.project.klare_server.common.error.UnauthorizedException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final BusinessUserRepository businessUserRepository;

    public AccountService(BusinessUserRepository businessUserRepository) {
        this.businessUserRepository = businessUserRepository;
    }

    @Transactional(readOnly = true)
    public MeResponse currentUser(UUID userId) {
        BusinessUser user = businessUserRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
        return new MeResponse(AuthUserResponse.from(user), CompanyResponse.from(user.getCompany()));
    }
}
