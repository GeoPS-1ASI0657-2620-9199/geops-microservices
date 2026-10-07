package com.geopslabs.geops.reservation.configuration;

import com.geopslabs.geops.reservation.application.services.ReservationCommandService;
import com.geopslabs.geops.reservation.application.services.ReservationQueryService;
import com.geopslabs.geops.reservation.application.usecases.CreateReservationUseCase;
import com.geopslabs.geops.reservation.domain.ports.OfferCatalogPort;
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
    public CreateReservationUseCase createReservationUseCase(ReservationRepositoryPort reservationRepository,
                                                             OfferCatalogPort offerCatalog, Clock clock,
                                                             RandomGenerator codeRandom) {
        return new ReservationCommandService(reservationRepository, offerCatalog, clock, codeRandom);
    }

    @Bean
    public ReservationQueryService reservationQueryService(ReservationRepositoryPort reservationRepository) {
        return new ReservationQueryService(reservationRepository);
    }
}
