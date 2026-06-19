package com.project.klare_server.employee.service;

import com.project.klare_server.auth.notification.EmailService;
import com.project.klare_server.common.error.ApiException;
import com.project.klare_server.common.error.ErrorCode;
import com.project.klare_server.common.error.ResourceNotFoundException;
import com.project.klare_server.company.domain.Company;
import com.project.klare_server.company.repository.CompanyRepository;
import com.project.klare_server.employee.domain.Employee;
import com.project.klare_server.employee.domain.EmployeeStatus;
import com.project.klare_server.employee.dto.ImportResultResponse;
import com.project.klare_server.employee.repository.EmployeeRepository;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EmployeeImportService {

    private static final int MAX_ROWS = 1000;
    private static final List<String> REQUIRED_HEADERS = List.of("name", "email", "phone", "salary");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9\\s-]{7,20}$");

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final EmailService emailService;

    public EmployeeImportService(
            EmployeeRepository employeeRepository,
            CompanyRepository companyRepository,
            EmailService emailService) {
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
        this.emailService = emailService;
    }

    @Transactional
    public ImportResultResponse importEmployees(UUID companyId, MultipartFile file, boolean sendInvitations) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "No file was uploaded");
        }
        List<ParsedRow> rows = parse(file);
        if (rows.size() > MAX_ROWS) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "File exceeds the maximum of " + MAX_ROWS + " rows");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        List<ImportResultResponse.RowError> errors = new ArrayList<>();
        Set<String> emailsInFile = new HashSet<>();
        List<Employee> toCreate = new ArrayList<>();

        for (ParsedRow row : rows) {
            try {
                Employee employee = validateRow(company, row, emailsInFile);
                toCreate.add(employee);
            } catch (RowException ex) {
                errors.add(new ImportResultResponse.RowError(row.rowNumber(), row.email(), ex.getMessage()));
            }
        }

        employeeRepository.saveAll(toCreate);
        employeeRepository.flush();

        if (sendInvitations) {
            for (Employee employee : toCreate) {
                emailService.sendEmployeeInvitation(employee.getEmail(), employee.getFirstName(), company.getName());
            }
        }

        return new ImportResultResponse(rows.size(), toCreate.size(), errors.size(), errors);
    }

    private Employee validateRow(Company company, ParsedRow row, Set<String> emailsInFile) {
        String name = safe(row.name());
        if (!StringUtils.hasText(name)) {
            throw new RowException("name is required");
        }
        String[] parts = name.split("\\s+", 2);
        if (parts.length < 2 || !StringUtils.hasText(parts[1])) {
            throw new RowException("name must include first and last name");
        }

        String email = safe(row.email()).toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new RowException("invalid email");
        }
        if (!emailsInFile.add(email)) {
            throw new RowException("duplicate email in file");
        }
        if (employeeRepository.existsByCompanyIdAndEmailIgnoreCase(company.getId(), email)) {
            throw new RowException("an employee with this email already exists");
        }

        String phone = safe(row.phone());
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new RowException("invalid phone number");
        }

        BigDecimal salary = parseSalary(row.salary());

        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setFirstName(parts[0].trim());
        employee.setLastName(parts[1].trim());
        employee.setEmail(email);
        employee.setPhone(phone);
        employee.setMonthlySalary(salary);
        employee.setStatus(EmployeeStatus.PENDING);
        return employee;
    }

    private BigDecimal parseSalary(String raw) {
        String cleaned = safe(raw).replaceAll("[^0-9.]", "");
        if (!StringUtils.hasText(cleaned)) {
            throw new RowException("invalid salary");
        }
        try {
            BigDecimal salary = new BigDecimal(cleaned);
            if (salary.signum() <= 0) {
                throw new RowException("salary must be greater than zero");
            }
            return salary.setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            throw new RowException("invalid salary");
        }
    }

    private List<ParsedRow> parse(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        try (InputStream input = file.getInputStream()) {
            if (filename.endsWith(".xlsx")) {
                return parseXlsx(input);
            }
            if (filename.endsWith(".csv") || "text/csv".equals(file.getContentType())) {
                return parseCsv(input);
            }
            throw new ApiException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Upload a .csv or .xlsx file");
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Could not read the uploaded file");
        }
    }

    private List<ParsedRow> parseCsv(InputStream input) throws Exception {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .get();
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            requireHeaders(parser.getHeaderMap().keySet());
            List<ParsedRow> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                int line = (int) record.getRecordNumber() + 1;
                rows.add(new ParsedRow(line, record.get("name"), record.get("email"), record.get("phone"), record.get("salary")));
            }
            return rows;
        }
    }

    private List<ParsedRow> parseXlsx(InputStream input) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "The spreadsheet is empty");
            }
            DataFormatter formatter = new DataFormatter();
            java.util.Map<String, Integer> columns = new java.util.HashMap<>();
            for (int c = header.getFirstCellNum(); c < header.getLastCellNum(); c++) {
                String key = formatter.formatCellValue(header.getCell(c)).trim().toLowerCase();
                if (StringUtils.hasText(key)) {
                    columns.put(key, c);
                }
            }
            requireHeaders(columns.keySet());

            List<ParsedRow> rows = new ArrayList<>();
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String name = cell(formatter, row, columns.get("name"));
                String email = cell(formatter, row, columns.get("email"));
                String phone = cell(formatter, row, columns.get("phone"));
                String salary = cell(formatter, row, columns.get("salary"));
                if (!StringUtils.hasText(name) && !StringUtils.hasText(email)
                        && !StringUtils.hasText(phone) && !StringUtils.hasText(salary)) {
                    continue;
                }
                rows.add(new ParsedRow(r + 1, name, email, phone, salary));
            }
            return rows;
        }
    }

    private void requireHeaders(Set<String> headers) {
        Set<String> normalized = new HashSet<>();
        for (String header : headers) {
            if (header != null) {
                normalized.add(header.trim().toLowerCase());
            }
        }
        if (!normalized.containsAll(REQUIRED_HEADERS)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "File must have columns: name, email, phone, salary");
        }
    }

    private String cell(DataFormatter formatter, Row row, Integer index) {
        if (index == null) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(index)).trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record ParsedRow(int rowNumber, String name, String email, String phone, String salary) {
    }

    private static class RowException extends RuntimeException {
        RowException(String message) {
            super(message);
        }
    }
}
