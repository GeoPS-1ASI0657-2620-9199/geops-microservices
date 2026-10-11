package com.geopslabs.geops.catalog.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.catalog.application.services.NearbyOfferQueryService;
import com.geopslabs.geops.catalog.application.usecases.SearchNearbyOffersUseCase;
import com.geopslabs.geops.catalog.domain.models.ProximitySearchService;
import com.geopslabs.geops.catalog.domain.ports.CellCachePort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import com.geopslabs.geops.catalog.infrastructure.cache.DisabledCellCache;
import com.geopslabs.geops.catalog.infrastructure.cache.RedisCellCacheAdapter;
import io.lettuce.core.ClientOptions;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;
import java.time.Duration;

/**
 * Búsqueda por proximidad with the Módulo de Caché por Celda (US14). geops.cache.enabled=false goes back to the
 * radius query of Sprint 1, the way back of the plan.
 */
@Configuration
public class ProximitySearchConfiguration {

    @Bean
    public CellCachePort cellCachePort(@Value("${geops.cache.enabled:true}") boolean enabled,
                                       StringRedisTemplate redis, ObjectMapper objectMapper, MeterRegistry meters,
                                       @Value("${geops.cache.cell-ttl:10m}") Duration cellTtl,
                                       @Value("${geops.cache.pause-after-failure:5s}") Duration pause) {
        if (!enabled) {
            return new DisabledCellCache();
        }
        return new RedisCellCacheAdapter(redis, objectMapper, meters, cellTtl, pause, System::nanoTime);
    }

    /** A disconnected client fails at once instead of queuing commands until the timeout. */
    @Bean
    public LettuceClientConfigurationBuilderCustomizer failFastRedisClient() {
        return builder -> builder.clientOptions(ClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .autoReconnect(true)
                .build());
    }

    @Bean
    public ProximitySearchService proximitySearchService(@Value("${geops.cache.enabled:true}") boolean enabled,
                                                         OfferRepositoryPort offerRepository,
                                                         CellCachePort cellCache, Clock clock) {
        return enabled ? new ProximitySearchService(offerRepository, cellCache, clock)
                : new ProximitySearchService(offerRepository, clock);
    }

    @Bean
    public SearchNearbyOffersUseCase searchNearbyOffersUseCase(ProximitySearchService proximitySearchService) {
        return new NearbyOfferQueryService(proximitySearchService);
    }
}
