package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.ModerationAction;
import com.geopslabs.geops.catalog.domain.models.ModerationCandidate;
import com.geopslabs.geops.catalog.domain.models.ModerationEntry;
import com.geopslabs.geops.catalog.domain.models.ModerationFilter;
import com.geopslabs.geops.catalog.domain.models.ResultPage;
import com.geopslabs.geops.catalog.domain.ports.ModerationRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.function.Function;

@Component
public class ModerationJpaAdapter implements ModerationRepositoryPort {
    private static final String ANY_TEXT = "%";
    private static final String ESCAPE = "!";

    private final ModerationActionJpaRepository repository;

    public ModerationJpaAdapter(ModerationActionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ModerationAction save(ModerationAction action) {
        var entity = new ModerationActionJpaEntity();
        entity.setOfferId(action.offerId());
        entity.setAdminId(action.adminId());
        entity.setReason(action.reason());
        entity.setRemovedAt(action.removedAt());
        return action.withId(repository.saveAndFlush(entity).getId());
    }

    @Override
    public ResultPage<ModerationCandidate> findOffers(ModerationFilter filter, int page, int size) {
        var rows = repository.findOffers(filter.statuses(), filter.reportedOnly(), patternOf(filter.text()),
                PageRequest.of(page, size));
        return pageOf(rows, row -> new ModerationCandidate(row.offerId(), row.title(), row.category(), row.source(),
                row.businessId(), row.businessName(), row.status(), row.validTo(), row.openReports(),
                row.verifiedSeal()));
    }

    @Override
    public ResultPage<ModerationEntry> findHistory(int page, int size) {
        return pageOf(repository.findHistory(PageRequest.of(page, size)), row -> new ModerationEntry(
                row.moderationId(), row.offerId(), row.offerTitle(), row.adminId(), row.reason(), row.removedAt()));
    }

    /** The text is matched literally: % and _ typed by the administrator are not wildcards. */
    static String patternOf(String text) {
        if (text == null) {
            return ANY_TEXT;
        }
        var literal = text.toLowerCase(Locale.ROOT)
                .replace(ESCAPE, ESCAPE + ESCAPE)
                .replace("%", ESCAPE + "%")
                .replace("_", ESCAPE + "_");
        return ANY_TEXT + literal + ANY_TEXT;
    }

    private static <R, T> ResultPage<T> pageOf(Page<R> rows, Function<R, T> mapper) {
        return new ResultPage<>(rows.getContent().stream().map(mapper).toList(), rows.getNumber(),
                rows.getTotalElements(), rows.getTotalPages());
    }
}
