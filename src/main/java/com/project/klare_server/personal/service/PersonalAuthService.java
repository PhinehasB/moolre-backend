package com.project.klare_server.personal.service;

import com.project.klare_server.common.config.properties.SecurityProperties;
import com.project.klare_server.common.error.ApiException;
import com.project.klare_server.common.error.ConflictException;
import com.project.klare_server.common.error.ErrorCode;
import com.project.klare_server.common.error.UnauthorizedException;
import com.project.klare_server.employee.domain.Employee;
import com.project.klare_server.employee.domain.EmployeeStatus;
import com.project.klare_server.employee.repository.EmployeeRepository;
import com.project.klare_server.personal.domain.PersonalAccountType;
import com.project.klare_server.personal.domain.PersonalRefreshToken;
import com.project.klare_server.personal.domain.PersonalUser;
import com.project.klare_server.personal.domain.PersonalUserStatus;
import com.project.klare_server.personal.dto.ActivateAccountRequest;
import com.project.klare_server.personal.dto.PersonalAccountResponse;
import com.project.klare_server.personal.dto.PersonalAuthResponse;
import com.project.klare_server.personal.dto.PersonalLoginRequest;
import com.project.klare_server.personal.dto.PersonalSignupRequest;
import com.project.klare_server.personal.repository.PersonalRefreshTokenRepository;
import com.project.klare_server.personal.repository.PersonalUserRepository;
import com.project.klare_server.personal.security.AuthenticatedPersonalUser;
import com.project.klare_server.personal.security.PersonalJwtService;
import java.time.Duration;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PersonalAuthService {

    private final EmployeeRepository employeeRepository;
    private final PersonalUserRepository personalUserRepository;
    private final PersonalRefreshTokenRepository refreshTokenRepository;
    private final PersonalRefreshTokenService refreshTokenService;
    private final PersonalLoginAttemptService loginAttemptService;
    private final PersonalJwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;

    public PersonalAuthService(
            EmployeeRepository employeeRepository,
            PersonalUserRepository personalUserRepository,
            PersonalRefreshTokenRepository refreshTokenRepository,
            PersonalRefreshTokenService refreshTokenService,
            PersonalLoginAttemptService loginAttemptService,
            PersonalJwtService jwtService,
            PasswordEncoder passwordEncoder,
            SecurityProperties securityProperties) {
        this.employeeRepository = employeeRepository;
        this.personalUserRepository = personalUserRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenService = refreshTokenService;
        this.loginAttemptService = loginAttemptService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.securityProperties = securityProperties;
    }

    @Transactional
    public PersonalAuthResponse signup(PersonalSignupRequest request, String userAgent, String ipAddress) {
        if (!request.acceptedTerms()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Please accept the terms and conditions to continue");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Passwords do not match");
        }
        validatePasswordStrength(request.password());

        String email = request.email().trim().toLowerCase();
        if (personalUserRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        PersonalUser user = new PersonalUser();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPhone(StringUtils.hasText(request.phone()) ? request.phone().trim() : null);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(PersonalUserStatus.ACTIVE);

        try {
            personalUserRepository.save(user);
            personalUserRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("An account with this email already exists");
        }

        return buildForIndividual(user, securityProperties.jwt().refreshTokenTtl(), userAgent, ipAddress);
    }

    @Transactional
    public PersonalAuthResponse login(PersonalLoginRequest request, String userAgent, String ipAddress) {
        String identifier = request.username().trim();
        Duration refreshTtl = request.rememberMe()
                ? securityProperties.jwt().refreshTokenTtl()
                : securityProperties.jwt().sessionTokenTtl();

        Employee employee = employeeRepository.findByUsername(identifier).orElse(null);
        if (employee != null) {
            return employeeLogin(employee, request.password(), refreshTtl, userAgent, ipAddress);
        }

        PersonalUser individual = personalUserRepository.findByEmailIgnoreCase(identifier).orElse(null);
        if (individual != null) {
            return individualLogin(individual, request.password(), refreshTtl, userAgent, ipAddress);
        }

        throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Invalid username or password");
    }

    @Transactional
    public PersonalAuthResponse activate(
            AuthenticatedPersonalUser principal, ActivateAccountRequest request, String userAgent, String ipAddress) {
        if (principal.accountType() != PersonalAccountType.EMPLOYEE) {
            throw new ApiException(ErrorCode.FORBIDDEN, "This account does not require activation");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Passwords do not match");
        }
        validatePasswordStrength(request.newPassword());

        Employee employee = employeeRepository.findById(principal.id())
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));

        employee.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        employee.setMustChangePassword(false);
        employee.setActivatedAt(Instant.now());
        if (employee.getStatus() == EmployeeStatus.PENDING) {
            employee.setStatus(EmployeeStatus.ACTIVE);
        }
        refreshTokenRepository.revokeAllForAccount(employee.getId(), PersonalAccountType.EMPLOYEE, Instant.now());

        return buildForEmployee(employee, securityProperties.jwt().refreshTokenTtl(), userAgent, ipAddress);
    }

    @Transactional
    public PersonalAuthResponse refresh(String rawRefreshToken, String userAgent, String ipAddress) {
        Instant now = Instant.now();
        PersonalRefreshToken current = refreshTokenRepository.findByTokenHash(refreshTokenService.hash(rawRefreshToken))
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Invalid refresh token"));

        if (current.getRevokedAt() != null) {
            refreshTokenService.revokeAllForAccount(current.getAccountId(), current.getAccountType());
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Refresh token has already been used");
        }
        if (!current.getExpiresAt().isAfter(now)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Refresh token has expired");
        }

        PersonalRefreshTokenService.IssuedToken rotated = refreshTokenService.issueUntil(
                current.getAccountId(), current.getAccountType(), userAgent, ipAddress, current.getExpiresAt());
        current.setRevokedAt(now);
        current.setReplacedByTokenHash(refreshTokenService.hash(rotated.rawToken()));

        if (current.getAccountType() == PersonalAccountType.EMPLOYEE) {
            Employee employee = employeeRepository.findById(current.getAccountId())
                    .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Account no longer exists"));
            if (employee.getStatus() == EmployeeStatus.SUSPENDED) {
                throw new ApiException(ErrorCode.ACCOUNT_INACTIVE, "Your account is suspended. Please contact your employer.");
            }
            String accessToken = jwtService.generateAccessToken(
                    employee.getId(), PersonalAccountType.EMPLOYEE, employee.getCompany().getId(), employee.isMustChangePassword());
            return response(accessToken, rotated, employee.isMustChangePassword(), PersonalAccountResponse.fromEmployee(employee));
        }

        PersonalUser user = personalUserRepository.findById(current.getAccountId())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Account no longer exists"));
        if (user.getStatus() == PersonalUserStatus.SUSPENDED) {
            throw new ApiException(ErrorCode.ACCOUNT_INACTIVE, "Your account is suspended.");
        }
        String accessToken = jwtService.generateAccessToken(user.getId(), PersonalAccountType.INDIVIDUAL, null, false);
        return response(accessToken, rotated, false, PersonalAccountResponse.fromIndividual(user));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(refreshTokenService.hash(rawRefreshToken))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    @Transactional(readOnly = true)
    public PersonalAccountResponse me(AuthenticatedPersonalUser principal) {
        if (principal.accountType() == PersonalAccountType.EMPLOYEE) {
            Employee employee = employeeRepository.findById(principal.id())
                    .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
            return PersonalAccountResponse.fromEmployee(employee);
        }
        PersonalUser user = personalUserRepository.findById(principal.id())
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
        return PersonalAccountResponse.fromIndividual(user);
    }

    private PersonalAuthResponse employeeLogin(
            Employee employee, String password, Duration refreshTtl, String userAgent, String ipAddress) {
        Instant now = Instant.now();
        if (employee.getLockedUntil() != null && employee.getLockedUntil().isAfter(now)) {
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED,
                    "Account temporarily locked after too many failed attempts. Please try again later.");
        }
        if (employee.getStatus() == EmployeeStatus.SUSPENDED) {
            throw new ApiException(ErrorCode.ACCOUNT_INACTIVE, "Your account is suspended. Please contact your employer.");
        }
        if (!StringUtils.hasText(employee.getPasswordHash())
                || !passwordEncoder.matches(password, employee.getPasswordHash())) {
            loginAttemptService.recordEmployeeFailure(employee.getId());
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Invalid username or password");
        }
        employee.setFailedLoginAttempts(0);
        employee.setLockedUntil(null);
        employee.setLastLoginAt(now);
        return buildForEmployee(employee, refreshTtl, userAgent, ipAddress);
    }

    private PersonalAuthResponse individualLogin(
            PersonalUser user, String password, Duration refreshTtl, String userAgent, String ipAddress) {
        Instant now = Instant.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED,
                    "Account temporarily locked after too many failed attempts. Please try again later.");
        }
        if (user.getStatus() == PersonalUserStatus.SUSPENDED) {
            throw new ApiException(ErrorCode.ACCOUNT_INACTIVE, "Your account is suspended.");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            loginAttemptService.recordIndividualFailure(user.getId());
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Invalid username or password");
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        return buildForIndividual(user, refreshTtl, userAgent, ipAddress);
    }

    private PersonalAuthResponse buildForEmployee(Employee employee, Duration refreshTtl, String userAgent, String ipAddress) {
        String accessToken = jwtService.generateAccessToken(
                employee.getId(), PersonalAccountType.EMPLOYEE, employee.getCompany().getId(), employee.isMustChangePassword());
        PersonalRefreshTokenService.IssuedToken refreshToken = refreshTokenService.issue(
                employee.getId(), PersonalAccountType.EMPLOYEE, userAgent, ipAddress, refreshTtl);
        return response(accessToken, refreshToken, employee.isMustChangePassword(), PersonalAccountResponse.fromEmployee(employee));
    }

    private PersonalAuthResponse buildForIndividual(PersonalUser user, Duration refreshTtl, String userAgent, String ipAddress) {
        String accessToken = jwtService.generateAccessToken(user.getId(), PersonalAccountType.INDIVIDUAL, null, false);
        PersonalRefreshTokenService.IssuedToken refreshToken = refreshTokenService.issue(
                user.getId(), PersonalAccountType.INDIVIDUAL, userAgent, ipAddress, refreshTtl);
        return response(accessToken, refreshToken, false, PersonalAccountResponse.fromIndividual(user));
    }

    private PersonalAuthResponse response(
            String accessToken,
            PersonalRefreshTokenService.IssuedToken refreshToken,
            boolean mustChangePassword,
            PersonalAccountResponse account) {
        return new PersonalAuthResponse(
                "Bearer",
                accessToken,
                jwtService.accessTokenTtlSeconds(),
                refreshToken.rawToken(),
                refreshToken.expiresAt(),
                mustChangePassword,
                account);
    }

    private void validatePasswordStrength(String password) {
        boolean longEnough = password.length() >= 8;
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!longEnough || !hasUpper || !hasLower || !hasDigit) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "Password must be at least 8 characters and include uppercase, lowercase, and a number");
        }
    }
}
