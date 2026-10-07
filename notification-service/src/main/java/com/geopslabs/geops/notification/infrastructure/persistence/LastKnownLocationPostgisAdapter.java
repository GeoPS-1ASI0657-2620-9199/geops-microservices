package com.geopslabs.geops.notification.infrastructure.persistence;

import com.geopslabs.geops.notification.domain.models.GeoPoint;
import com.geopslabs.geops.notification.domain.models.LastKnownLocation;
import com.geopslabs.geops.notification.domain.ports.LastKnownLocationRepositoryPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Component
public class LastKnownLocationPostgisAdapter implements LastKnownLocationRepositoryPort {
    private static final int SRID_WGS84 = 4326;
    private static final String CONSUMER_ID = "consumerId";
    private static final String LATITUDE = "latitude";
    private static final String LONGITUDE = "longitude";
    private static final String ACCURACY = "accuracy";
    private static final String RADIUS = "radius";
    private static final String CAPTURED_AT = "capturedAt";
    private static final String SRID = "srid";
    private static final String SAVE_IF_NEWER = """
            INSERT INTO last_known_locations (consumer_id, position, accuracy_m, radius_m, captured_at)
            VALUES (:consumerId, ST_SetSRID(ST_MakePoint(:longitude, :latitude), :srid)::geography, :accuracy,
                    :radius, :capturedAt)
            ON CONFLICT (consumer_id) DO UPDATE SET position = EXCLUDED.position, accuracy_m = EXCLUDED.accuracy_m,
                captured_at = EXCLUDED.captured_at
            WHERE last_known_locations.captured_at < EXCLUDED.captured_at""";
    private static final String FIND_BY_CONSUMER = """
            SELECT ST_Y(position::geometry) AS latitude, ST_X(position::geometry) AS longitude, accuracy_m,
                   radius_m, captured_at
            FROM last_known_locations WHERE consumer_id = :consumerId""";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public LastKnownLocationPostgisAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public LastKnownLocation saveIfNewer(Long consumerId, LastKnownLocation location) {
        var parameters = new MapSqlParameterSource()
                .addValue(CONSUMER_ID, consumerId)
                .addValue(LATITUDE, location.position().latitude())
                .addValue(LONGITUDE, location.position().longitude())
                .addValue(SRID, SRID_WGS84)
                .addValue(ACCURACY, location.accuracyMeters())
                .addValue(RADIUS, location.radiusMeters())
                .addValue(CAPTURED_AT, location.capturedAt().atOffset(ZoneOffset.UTC));
        jdbcTemplate.update(SAVE_IF_NEWER, parameters);
        return findByConsumerId(consumerId).orElseThrow();
    }

    @Override
    public Optional<LastKnownLocation> findByConsumerId(Long consumerId) {
        var parameters = new MapSqlParameterSource(CONSUMER_ID, consumerId);
        return jdbcTemplate.query(FIND_BY_CONSUMER, parameters, LastKnownLocationPostgisAdapter::toLocation)
                .stream()
                .findFirst();
    }

    private static LastKnownLocation toLocation(ResultSet row, int rowNumber) throws SQLException {
        var capturedAt = row.getObject("captured_at", OffsetDateTime.class);
        return new LastKnownLocation(new GeoPoint(row.getDouble(LATITUDE), row.getDouble(LONGITUDE)),
                row.getInt("accuracy_m"), row.getInt("radius_m"),
                LocalDateTime.ofInstant(capturedAt.toInstant(), ZoneOffset.UTC));
    }
}
