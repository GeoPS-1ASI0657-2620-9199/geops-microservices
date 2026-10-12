package com.geopslabs.geops.identity.infrastructure.sunat;

import com.geopslabs.geops.identity.domain.models.Ruc;
import com.geopslabs.geops.identity.domain.models.Taxpayer;
import com.geopslabs.geops.identity.domain.ports.TaxpayerRegistryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Reads Identity's copy of the registry in sunat_taxpayers. Nothing of SUNAT's file format reaches the domain.
 */
@Component
public class SunatTaxpayerRegistryAdapter implements TaxpayerRegistryPort {
    private static final String FIND_BY_RUC = """
            SELECT ruc, business_name, taxpayer_status, taxpayer_condition
              FROM sunat_taxpayers
             WHERE ruc = ?""";
    private static final String ANY_ROW = "SELECT EXISTS (SELECT 1 FROM sunat_taxpayers)";

    private final JdbcTemplate jdbc;

    public SunatTaxpayerRegistryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Taxpayer> findByRuc(Ruc ruc) {
        return jdbc.query(FIND_BY_RUC, (row, index) -> new Taxpayer(new Ruc(row.getString("ruc")),
                        row.getString("business_name"), row.getString("taxpayer_status"),
                        row.getString("taxpayer_condition")), ruc.number())
                .stream()
                .findFirst();
    }

    @Override
    public boolean isLoaded() {
        return Boolean.TRUE.equals(jdbc.queryForObject(ANY_ROW, Boolean.class));
    }
}
