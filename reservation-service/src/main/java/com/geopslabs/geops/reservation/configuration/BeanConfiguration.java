package com.geopslabs.geops.reservation.configuration;

import com.geopslabs.geops.backend.identity.infrastructure.persistence.jpa.UserRepository;
import com.geopslabs.geops.backend.payments.infrastructure.persistence.jpa.PaymentRepository;
import com.geopslabs.geops.reservation.application.services.ReservationCommandService;
import com.geopslabs.geops.reservation.application.services.ReservationQueryService;
import com.geopslabs.geops.reservation.application.usecases.ReservationCommandUseCase;
import com.geopslabs.geops.reservation.application.usecases.ReservationQueryUseCase;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public ReservationCommandUseCase reservationCommandUseCase(ReservationRepositoryPort reservationRepository,
                                                     UserRepository userRepository,
                                                     PaymentRepository paymentRepository) {
        return new ReservationCommandService(reservationRepository, userRepository, paymentRepository);
    }

    @Bean
    public ReservationQueryUseCase reservationQueryUseCase(ReservationRepositoryPort reservationRepository) {
        return new ReservationQueryService(reservationRepository);
    }
}
