package com.project.klare_server.auth.service;

import com.project.klare_server.auth.domain.BusinessUser;
import com.project.klare_server.auth.domain.BusinessUserRole;
import com.project.klare_server.auth.domain.BusinessUserStatus;
import com.project.klare_server.auth.domain.RefreshToken;
import com.project.klare_server.auth.dto.AuthUserResponse;
import com.project.klare_server.auth.dto.AuthenticationResponse;
import com.project.klare_server.auth.dto.CompanyResponse;
import com.project.klare_server.auth.dto.ForgotPasswordRequest;
import com.project.klare_server.auth.dto.LoginRequest;
import com.project.klare_server.auth.dto.RegisterCompanyRequest;
import com.project.klare_server.auth.dto.ResetPasswordRequest;
import com.project.klare_server.auth.notification.EmailService;
import com.project.klare_server.auth.repository.BusinessUserRepository;
import com.project.klare_server.auth.repository.RefreshTokenRepository;
import com.project.klare_server.common.config.properties.AppProperties;
import com.project.klare_server.common.config.properties.SecurityProperties;
import com.project.klare_server.common.error.ApiException;
import com.project.klare_server.common.error.ConflictException;
import com.project.klare_server.common.error.ErrorCode;
import com.project.klare_server.company.domain.Company;
import com.project.klare_server.company.domain.CompanyStatus;
import com.project.klare_server.company.domain.CompanyWallet;
import com.project.klare_server.company.repository.CompanyRepository;
import com.project.klare_server.company.repository.CompanyWalletRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final CompanyRepository companyRepository;
    private final CompanyWalletRepository companyWalletRepository;
    private final BusinessUserRepository businessUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetService passwordResetService;
    private final LoginAttemptService loginAttemptService;
    private final EmailService emailService;
    private final SecurityProperties securityProperties;
    private final AppProperties appProperties;

    public AuthService(
            CompanyRepository companyRepository,
            CompanyWalletRepository companyWalletRepository,
            BusinessUserRepository businessUserRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            PasswordResetService passwordResetService,
            LoginAttemptService loginAttemptService,
            EmailService emailService,
            SecurityProperties securityProperties,
            AppProperties appProperties) {
        this.companyRepository = companyRepository;
        this.companyWalletRepository = companyWalletRepository;
        this.businessUserRepository = businessUserRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordResetService = passwordResetService;
        this.loginAttemptService = loginAttemptService;
        this.emailService = emailService;
        this.securityProperties = securityProperties;
        this.appProperties = appProperties;
    }

    @Transactional
    public AuthenticationResponse registerCompany(RegisterCompanyRequest request, String userAgent, String ipAddress) {
        String email = normalizeEmail(request.administrator().email());
        String registrationNumber = normalizeRegistrationNumber(request.company().registrationNumber());

        if (businessUserRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        if (companyRepository.existsByRegistrationNumber(registrationNumber)) {
            throw new ConflictException("A company with this registration number already exists");
        }

        Instant now = Instant.now();
        Company company = new Company();
        company.setName(request.company().name().trim());
        company.setRegistrationNumber(registrationNumber);
        company.setIndustry(request.company().industry());
        company.setExpectedMonthlyPayroll(request.company().expectedMonthlyPayroll());
        company.setStatus(CompanyStatus.PENDING_VERIFICATION);
        company.setTermsAcceptedAt(now);
        company.setFundsAuthorizationAcceptedAt(now);

        BusinessUser admin = new BusinessUser();
        admin.setCompany(company);
        admin.setFirstName(request.administrator().firstName().trim());
        admin.setLastName(request.administrator().lastName().trim());
        admin.setEmail(email);
        admin.setPhone(request.administrator().phone().trim());
        admin.setPasswordHash(passwordEncoder.encode(request.administrator().password()));
        admin.setRole(BusinessUserRole.OWNER);
        admin.setStatus(BusinessUserStatus.ACTIVE);
        admin.setEmailVerified(false);

        CompanyWallet wallet = new CompanyWallet();
        wallet.setCompany(company);

        try {
            companyRepository.save(company);
            companyWalletRepository.save(wallet);
            businessUserRepository.save(admin);
            businessUserRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("An account with these details already exists");
        }

        return buildAuthResponse(admin, company, securityProperties.jwt().refreshTokenTtl(), userAgent, ipAddress);
    }

    @Transactional
    public AuthenticationResponse login(LoginRequest request, String userAgent, String ipAddress) {
        String email = normalizeEmail(request.email());
        BusinessUser user = businessUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password"));

        Instant now = Instant.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED,
                    "Account temporarily locked after too many failed attempts. Please try again later.");
        }
        if (user.getStatus() != BusinessUserStatus.ACTIVE) {
            throw new ApiException(ErrorCode.ACCOUNT_INACTIVE, "Your account is not active. Please contact your administrator.");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(user.getId());
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);

        Duration refreshTtl = request.rememberMe()
                ? securityProperties.jwt().refreshTokenTtl()
                : securityProperties.jwt().sessionTokenTtl();
        return buildAuthResponse(user, user.getCompany(), refreshTtl, userAgent, ipAddress);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        businessUserRepository.findByEmailIgnoreCase(email)
                .filter(user -> user.getStatus() == BusinessUserStatus.ACTIVE)
                .ifPresent(user -> {
                    String rawToken = passwordResetService.issue(user);
                    String resetLink = appProperties.passwordResetUrl() + "?token=" + rawToken;
                    emailService.sendPasswordReset(user.getEmail(), user.getFirstName(), resetLink);
                });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        BusinessUser user = passwordResetService.consume(request.token());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        refreshTokenRepository.revokeAllForUser(user, Instant.now());
    }

    @Transactional
    public AuthenticationResponse refresh(String rawRefreshToken, String userAgent, String ipAddress) {
        Instant now = Instant.now();
        RefreshToken current = refreshTokenRepository.findByTokenHash(refreshTokenService.hash(rawRefreshToken))
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Invalid refresh token"));

        if (current.getRevokedAt() != null) {
            refreshTokenService.revokeAllForUser(current.getUser());
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Refresh token has already been used");
        }
        if (!current.getExpiresAt().isAfter(now)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Refresh token has expired");
        }

        BusinessUser user = current.getUser();
        if (user.getStatus() != BusinessUserStatus.ACTIVE) {
            throw new ApiException(ErrorCode.ACCOUNT_INACTIVE, "Your account is not active. Please contact your administrator.");
        }

        RefreshTokenService.IssuedToken rotated = refreshTokenService.issueUntil(user, userAgent, ipAddress, current.getExpiresAt());
        current.setRevokedAt(now);
        current.setReplacedByTokenHash(refreshTokenService.hash(rotated.rawToken()));

        String accessToken = jwtService.generateAccessToken(user);
        return new AuthenticationResponse(
                "Bearer",
                accessToken,
                jwtService.accessTokenTtlSeconds(),
                rotated.rawToken(),
                rotated.expiresAt(),
                AuthUserResponse.from(user),
                CompanyResponse.from(user.getCompany()));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(refreshTokenService.hash(rawRefreshToken))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    private AuthenticationResponse buildAuthResponse(
            BusinessUser user, Company company, Duration refreshTtl, String userAgent, String ipAddress) {
        String accessToken = jwtService.generateAccessToken(user);
        RefreshTokenService.IssuedToken refreshToken = refreshTokenService.issue(user, userAgent, ipAddress, refreshTtl);
        return new AuthenticationResponse(
                "Bearer",
                accessToken,
                jwtService.accessTokenTtlSeconds(),
                refreshToken.rawToken(),
                refreshToken.expiresAt(),
                AuthUserResponse.from(user),
                CompanyResponse.from(company));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizeRegistrationNumber(String registrationNumber) {
        return registrationNumber.trim().toUpperCase();
    }
}
