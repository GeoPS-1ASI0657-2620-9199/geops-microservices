package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.BusinessBaseline;
import com.geopslabs.geops.catalog.domain.ports.BusinessBaselineRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Optional;

/** business_baselines: one row per business; declaring again replaces it in one upsert. */
@Component
public class BusinessBaselineJdbcAdapter implements BusinessBaselineRepositoryPort {
    private static final String UPSERT = """
            INSERT INTO business_baselines (business_id, customers_per_week, average_ticket, declared_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (business_id) DO UPDATE
            SET customers_per_week = EXCLUDED.customers_per_week,
                average_ticket = EXCLUDED.average_ticket,
                declared_at = EXCLUDED.declared_at
            """;
    private static final String FIND = """
            SELECT business_id, customers_per_week, average_ticket, declared_at
            FROM business_baselines WHERE business_id = ?
            """;

    private final JdbcTemplate jdbc;

    public BusinessBaselineJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public BusinessBaseline save(BusinessBaseline baseline) {
        jdbc.update(UPSERT, baseline.businessId(), baseline.customersPerWeek(), baseline.averageTicket(),
                Timestamp.from(baseline.declaredAt()));
        return baseline;
    }

    @Override
    public Optional<BusinessBaseline> findByBusinessId(Long businessId) {
        return jdbc.query(FIND, (row, index) -> new BusinessBaseline(row.getLong("business_id"),
                        row.getInt("customers_per_week"), row.getBigDecimal("average_ticket"),
                        row.getTimestamp("declared_at").toInstant()), businessId)
                .stream().findFirst();
    }
}
