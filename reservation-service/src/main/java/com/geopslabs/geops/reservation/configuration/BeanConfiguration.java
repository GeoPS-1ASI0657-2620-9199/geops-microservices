package com.geopslabs.geops.reservation.configuration;

import com.geopslabs.geops.reservation.application.services.ReservationCommandService;
import com.geopslabs.geops.reservation.application.services.ReservationQueryService;
import com.geopslabs.geops.reservation.domain.ports.OfferCatalogPort;
import com.geopslabs.geops.reservation.domain.ports.OutboxPort;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.random.RandomGenerator;

@Configuration
public class BeanConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public RandomGenerator codeRandom() {
        return new SecureRandom();
    }

    @Bean
    public ReservationCommandService reservationCommandService(ReservationRepositoryPort reservationRepository,
                                                               OfferCatalogPort offerCatalog, OutboxPort outbox,
                                                               Clock clock, RandomGenerator codeRandom) {
        return new ReservationCommandService(reservationRepository, offerCatalog, outbox, clock, codeRandom);
    }

    @Bean
    public ReservationQueryService reservationQueryService(ReservationRepositoryPort reservationRepository) {
        return new ReservationQueryService(reservationRepository);
    }
}
