package com.project.klare_server.settings.service;

import com.project.klare_server.auth.domain.BusinessUser;
import com.project.klare_server.auth.repository.BusinessUserRepository;
import com.project.klare_server.auth.repository.RefreshTokenRepository;
import com.project.klare_server.common.error.ApiException;
import com.project.klare_server.common.error.ConflictException;
import com.project.klare_server.common.error.ErrorCode;
import com.project.klare_server.common.error.ResourceNotFoundException;
import com.project.klare_server.common.error.UnauthorizedException;
import com.project.klare_server.company.domain.Company;
import com.project.klare_server.company.repository.CompanyRepository;
import com.project.klare_server.settings.dto.ChangePasswordRequest;
import com.project.klare_server.settings.dto.SettingsResponse;
import com.project.klare_server.settings.dto.UpdateCompanyProfileRequest;
import com.project.klare_server.settings.dto.UpdatePayrollAutomationRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    private final BusinessUserRepository businessUserRepository;
    private final CompanyRepository companyRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public SettingsService(
            BusinessUserRepository businessUserRepository,
            CompanyRepository companyRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder) {
        this.businessUserRepository = businessUserRepository;
        this.companyRepository = companyRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public SettingsResponse getSettings(UUID userId) {
        BusinessUser user = loadUser(userId);
        return toResponse(user, user.getCompany());
    }

    @Transactional
    public SettingsResponse updatePayrollAutomation(UUID companyId, UpdatePayrollAutomationRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        company.setAutoPayrollEnabled(request.automaticPayroll());
        company.setPayrollDayOfMonth(request.payDate());
        company.setEmailEstimateBeforeRun(request.emailEstimateBeforeRun());
        company.setNotifyEmployeesBeforePayday(request.notifyEmployeesBeforePayday());
        return new SettingsResponse(toPayrollAutomation(company), null);
    }

    @Transactional
    public SettingsResponse updateCompanyProfile(UUID userId, UpdateCompanyProfileRequest request) {
        BusinessUser user = loadUser(userId);
        Company company = user.getCompany();

        String registrationNumber = request.registrationNumber().trim().toUpperCase();
        if (!registrationNumber.equals(company.getRegistrationNumber())
                && companyRepository.existsByRegistrationNumber(registrationNumber)) {
            throw new ConflictException("A company with this registration number already exists");
        }

        String adminEmail = request.adminEmail().trim().toLowerCase();
        if (!adminEmail.equalsIgnoreCase(user.getEmail())
                && businessUserRepository.existsByEmailIgnoreCase(adminEmail)) {
            throw new ConflictException("An account with this email already exists");
        }

        company.setName(request.companyName().trim());
        company.setRegistrationNumber(registrationNumber);
        user.setEmail(adminEmail);

        try {
            businessUserRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("These details are already in use");
        }

        return new SettingsResponse(null, toCompanyProfile(user, company));
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        BusinessUser user = loadUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.revokeAllForUser(user, Instant.now());
    }

    private BusinessUser loadUser(UUID userId) {
        return businessUserRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
    }

    private SettingsResponse toResponse(BusinessUser user, Company company) {
        return new SettingsResponse(toPayrollAutomation(company), toCompanyProfile(user, company));
    }

    private SettingsResponse.PayrollAutomation toPayrollAutomation(Company company) {
        return new SettingsResponse.PayrollAutomation(
                company.isAutoPayrollEnabled(),
                company.getPayrollDayOfMonth(),
                company.isEmailEstimateBeforeRun(),
                company.isNotifyEmployeesBeforePayday());
    }

    private SettingsResponse.CompanyProfile toCompanyProfile(BusinessUser user, Company company) {
        return new SettingsResponse.CompanyProfile(
                company.getName(), company.getRegistrationNumber(), user.getEmail());
    }
}
