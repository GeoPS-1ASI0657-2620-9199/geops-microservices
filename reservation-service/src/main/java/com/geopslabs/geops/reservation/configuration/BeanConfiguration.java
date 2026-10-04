package com.geopslabs.geops.reservation.configuration;

import com.geopslabs.geops.backend.identity.infrastructure.persistence.jpa.UserRepository;
import com.geopslabs.geops.backend.payments.infrastructure.persistence.jpa.PaymentRepository;
import com.geopslabs.geops.reservation.application.services.CouponCommandService;
import com.geopslabs.geops.reservation.application.services.CouponQueryService;
import com.geopslabs.geops.reservation.application.usecases.CouponCommandUseCase;
import com.geopslabs.geops.reservation.application.usecases.CouponQueryUseCase;
import com.geopslabs.geops.reservation.domain.ports.CouponRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public CouponCommandUseCase couponCommandUseCase(CouponRepositoryPort couponRepository,
                                                     UserRepository userRepository,
                                                     PaymentRepository paymentRepository) {
        return new CouponCommandService(couponRepository, userRepository, paymentRepository);
    }

    @Bean
    public CouponQueryUseCase couponQueryUseCase(CouponRepositoryPort couponRepository) {
        return new CouponQueryService(couponRepository);
    }
}
