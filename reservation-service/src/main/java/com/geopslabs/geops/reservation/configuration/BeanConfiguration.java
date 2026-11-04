package com.geopslabs.geops.reservation.configuration;

import com.geopslabs.geops.reservation.application.services.DeadlineScheduler;
import com.geopslabs.geops.reservation.application.services.ReportCommandService;
import com.geopslabs.geops.reservation.application.services.ReportQueryService;
import com.geopslabs.geops.reservation.application.services.ReservationCommandService;
import com.geopslabs.geops.reservation.application.services.ReservationQueryService;
import com.geopslabs.geops.reservation.domain.ports.JobLockPort;
import com.geopslabs.geops.reservation.domain.ports.NonComplianceReportRepositoryPort;
import com.geopslabs.geops.reservation.domain.ports.OfferCatalogPort;
import com.geopslabs.geops.reservation.domain.ports.OutboxPort;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
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

    @Bean
    public ReportCommandService reportCommandService(ReservationRepositoryPort reservations,
                                                     NonComplianceReportRepositoryPort reports, OutboxPort outbox,
                                                     Clock clock,
                                                     @Value("${geops.reports.response-window:48h}") Duration window) {
        return new ReportCommandService(reservations, reports, outbox, clock, UUID::randomUUID, window);
    }

    @Bean
    public ReportQueryService reportQueryService(NonComplianceReportRepositoryPort reports,
                                                 ReservationRepositoryPort reservations) {
        return new ReportQueryService(reports, reservations);
    }

    @Bean
    public DeadlineScheduler deadlineScheduler(ReservationRepositoryPort reservations,
                                               NonComplianceReportRepositoryPort reports, OutboxPort outbox,
                                               JobLockPort lock, Clock clock,
                                               PlatformTransactionManager transactionManager) {
        return new DeadlineScheduler(reservations, reports, outbox, lock, clock, UUID::randomUUID, transactionManager);
    }
}
