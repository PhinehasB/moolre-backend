package com.project.klare_server.transactions.service;

import com.project.klare_server.auth.domain.BusinessUser;
import com.project.klare_server.auth.repository.BusinessUserRepository;
import com.project.klare_server.common.error.UnauthorizedException;
import com.project.klare_server.common.web.PageResponse;
import com.project.klare_server.transactions.dto.TransactionFilter;
import com.project.klare_server.wallet.dto.LedgerEntry;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TransactionService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private final BusinessUserRepository businessUserRepository;
    private final LedgerAssembler ledgerAssembler;

    public TransactionService(BusinessUserRepository businessUserRepository, LedgerAssembler ledgerAssembler) {
        this.businessUserRepository = businessUserRepository;
        this.ledgerAssembler = ledgerAssembler;
    }

    @Transactional(readOnly = true)
    public PageResponse<LedgerEntry> list(UUID userId, TransactionFilter filter, String query, int page, int size) {
        List<LedgerEntry> filtered = filtered(userId, filter, query);
        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        List<LedgerEntry> content = filtered.subList(from, to);
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) filtered.size() / size);
        boolean last = to >= filtered.size();
        return new PageResponse<>(content, page, size, filtered.size(), totalPages, last);
    }

    @Transactional(readOnly = true)
    public ReportFile export(UUID userId, TransactionFilter filter, String query) {
        List<LedgerEntry> entries = filtered(userId, filter, query);
        StringBuilder csv = new StringBuilder("date,description,reference,status,direction,amount\n");
        for (LedgerEntry entry : entries) {
            csv.append(DATE.format(entry.date())).append(',')
                    .append('"').append(entry.description().replace("\"", "\"\"")).append('"').append(',')
                    .append(entry.reference()).append(',')
                    .append(entry.status()).append(',')
                    .append(entry.direction()).append(',')
                    .append(entry.amount().toPlainString())
                    .append('\n');
        }
        return new ReportFile("transactions.csv", csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private List<LedgerEntry> filtered(UUID userId, TransactionFilter filter, String query) {
        BusinessUser user = businessUserRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
        UUID companyId = user.getCompany().getId();
        boolean live = user.getCompany().isLiveMode();
        TransactionFilter effective = filter == null ? TransactionFilter.ALL : filter;
        String term = StringUtils.hasText(query) ? query.trim().toLowerCase(Locale.ROOT) : null;

        return ledgerAssembler.assemble(companyId, live).stream()
                .filter(entry -> matchesFilter(entry, effective))
                .filter(entry -> matchesQuery(entry, term))
                .toList();
    }

    private boolean matchesFilter(LedgerEntry entry, TransactionFilter filter) {
        return switch (filter) {
            case ALL -> true;
            case INFLOWS -> "CREDIT".equals(entry.direction());
            case PAYOUTS -> "DEBIT".equals(entry.direction());
            case FAILED -> "Failed".equalsIgnoreCase(entry.status());
        };
    }

    private boolean matchesQuery(LedgerEntry entry, String term) {
        if (term == null) {
            return true;
        }
        return entry.description().toLowerCase(Locale.ROOT).contains(term)
                || entry.reference().toLowerCase(Locale.ROOT).contains(term);
    }

    public record ReportFile(String filename, byte[] content) {
    }
}
