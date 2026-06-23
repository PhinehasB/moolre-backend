package com.project.klare_server.employee.service;

import com.project.klare_server.common.error.ConflictException;
import com.project.klare_server.common.error.ResourceNotFoundException;
import com.project.klare_server.common.web.PageResponse;
import com.project.klare_server.company.domain.Company;
import com.project.klare_server.company.repository.CompanyRepository;
import com.project.klare_server.employee.domain.Employee;
import com.project.klare_server.employee.domain.EmployeeStatus;
import com.project.klare_server.employee.dto.CreateEmployeeRequest;
import com.project.klare_server.employee.dto.EmployeeResponse;
import com.project.klare_server.employee.dto.EmployeeStatsResponse;
import com.project.klare_server.employee.dto.UpdateEmployeeRequest;
import com.project.klare_server.employee.repository.EmployeeRepository;
import com.project.klare_server.notification.NotificationService;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final NotificationService notificationService;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            CompanyRepository companyRepository,
            NotificationService notificationService) {
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public EmployeeResponse create(UUID companyId, CreateEmployeeRequest request) {
        String email = request.email().trim().toLowerCase();
        if (employeeRepository.existsByCompanyIdAndEmailIgnoreCase(companyId, email)) {
            throw new ConflictException("An employee with this email already exists");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setEmail(email);
        employee.setPhone(request.phone().trim());
        employee.setRole(request.role().trim());
        employee.setMonthlySalary(request.monthlySalary());
        employee.setStatus(EmployeeStatus.PENDING);

        try {
            employeeRepository.save(employee);
            employeeRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("An employee with this email already exists");
        }

        if (request.sendInvitation() == null || request.sendInvitation()) {
            notificationService.employeeInvitation(employee.getEmail(), employee.getPhone(),
                    employee.getFirstName(), company.getName());
        }

        return EmployeeResponse.from(employee);
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> list(UUID companyId, String query, EmployeeStatus status, Pageable pageable) {
        String term = StringUtils.hasText(query) ? "%" + query.trim().toLowerCase() + "%" : null;
        return PageResponse.from(employeeRepository.search(companyId, status, term, pageable).map(EmployeeResponse::from));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(UUID companyId, UUID employeeId) {
        return EmployeeResponse.from(loadOwned(companyId, employeeId));
    }

    @Transactional(readOnly = true)
    public EmployeeStatsResponse stats(UUID companyId) {
        return new EmployeeStatsResponse(
                employeeRepository.countByCompanyId(companyId),
                employeeRepository.countByCompanyIdAndStatus(companyId, EmployeeStatus.ACTIVE),
                employeeRepository.countByCompanyIdAndStatus(companyId, EmployeeStatus.PENDING),
                employeeRepository.countByCompanyIdAndStatus(companyId, EmployeeStatus.SUSPENDED));
    }

    @Transactional
    public EmployeeResponse update(UUID companyId, UUID employeeId, UpdateEmployeeRequest request) {
        Employee employee = loadOwned(companyId, employeeId);
        String email = request.email().trim().toLowerCase();
        if (!email.equalsIgnoreCase(employee.getEmail())
                && employeeRepository.existsByCompanyIdAndEmailIgnoreCase(companyId, email)) {
            throw new ConflictException("An employee with this email already exists");
        }

        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setEmail(email);
        employee.setPhone(request.phone().trim());
        employee.setRole(request.role().trim());
        employee.setMonthlySalary(request.monthlySalary());
        employee.setStatus(request.status());

        try {
            employeeRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("An employee with this email already exists");
        }

        return EmployeeResponse.from(employee);
    }

    private Employee loadOwned(UUID companyId, UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        if (!employee.getCompany().getId().equals(companyId)) {
            throw new ResourceNotFoundException("Employee not found");
        }
        return employee;
    }
}
