package com.geopslabs.geops.reservation.application.services;

import com.geopslabs.geops.reservation.application.usecases.CreateReservationUseCase;
import com.geopslabs.geops.reservation.application.usecases.RedeemReservationUseCase;
import com.geopslabs.geops.reservation.application.usecases.ReservationResult;
import com.geopslabs.geops.reservation.domain.models.OfferSnapshot;
import com.geopslabs.geops.reservation.domain.models.RedeemRejection;
import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.ReservationCode;
import com.geopslabs.geops.reservation.domain.models.commands.CreateReservationCommand;
import com.geopslabs.geops.reservation.domain.models.commands.RedeemReservationCommand;
import com.geopslabs.geops.reservation.domain.models.exceptions.ActiveReservationAlreadyExistsException;
import com.geopslabs.geops.reservation.domain.models.exceptions.OfferNotAvailableException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReservationNotFoundException;
import com.geopslabs.geops.reservation.domain.ports.OfferCatalogPort;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

public class ReservationCommandService implements CreateReservationUseCase, RedeemReservationUseCase {
    public static final ZoneId OFFER_ZONE = ZoneId.of("America/Lima");
    public static final int MAX_CODE_ATTEMPTS = 5;
    private static final LocalTime END_OF_DAY = LocalTime.MAX.truncatedTo(ChronoUnit.SECONDS);
    private static final String CODES_EXHAUSTED_MESSAGE = "No free reservation code after %d attempts";
    private static final int CODE_SUFFIX_LENGTH = 3;
    private static final String UNEXPLAINED_REJECTION = "Reservation %d was not redeemed and breaks no rule";
    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationCommandService.class);

    private final ReservationRepositoryPort reservationRepository;
    private final OfferCatalogPort offerCatalog;
    private final Clock clock;
    private final RandomGenerator random;

    public ReservationCommandService(ReservationRepositoryPort reservationRepository, OfferCatalogPort offerCatalog,
                                     Clock clock, RandomGenerator random) {
        this.reservationRepository = reservationRepository;
        this.offerCatalog = offerCatalog;
        this.clock = clock;
        this.random = random;
    }

    @Override
    public ReservationResult create(CreateReservationCommand command) {
        var offer = offerCatalog.findValidOffer(command.offerId());
        if (offer.validTo().isBefore(LocalDate.now(clock.withZone(OFFER_ZONE)))) {
            throw new OfferNotAvailableException(offer.offerId());
        }
        var existing = reservationRepository.findActiveByConsumerAndOffer(command.consumerId(), command.offerId());
        if (existing.isPresent()) {
            LOGGER.info("reservation.reused reservationId={} offerId={}", existing.get().getId(), offer.offerId());
            return ReservationResult.existing(existing.get());
        }
        return insert(command, offer);
    }

    /**
     * The conditional update decides; the reservation is only read afterwards, to return it or to explain
     * the rejection.
     */
    @Override
    @Transactional
    public Reservation redeem(RedeemReservationCommand command) {
        var now = now();
        if (reservationRepository.redeemIfActive(command.code(), command.businessId(), now) == 1) {
            var redeemed = reservationRepository.findByCode(command.code())
                    .orElseThrow(() -> ReservationNotFoundException.withCode(command.code()));
            LOGGER.info("reservation.redeemed reservationId={} businessId={}", redeemed.getId(), command.businessId());
            return redeemed;
        }
        throw rejection(command, now);
    }

    private RuntimeException rejection(RedeemReservationCommand command, LocalDateTime now) {
        var stored = reservationRepository.findByCode(command.code());
        if (stored.isEmpty()) {
            logRejection(RedeemRejection.NOT_FOUND, command);
            return ReservationNotFoundException.withCode(command.code());
        }
        var reservation = stored.get();
        var reason = reservation.rejectionFor(command.businessId(), now)
                .orElseThrow(() -> new IllegalStateException(UNEXPLAINED_REJECTION.formatted(reservation.getId())));
        logRejection(reason, command);
        return reservation.rejectionException(reason);
    }

    private static void logRejection(RedeemRejection reason, RedeemReservationCommand command) {
        var code = command.code();
        LOGGER.warn("reservation.redeem.rejected reason={} businessId={} codeSuffix={}", reason, command.businessId(),
                code.substring(code.length() - CODE_SUFFIX_LENGTH));
    }

    private ReservationResult insert(CreateReservationCommand command, OfferSnapshot offer) {
        var reservation = Reservation.create(offer, command.consumerId(), newCode(), now(), endOfValidity(offer));
        try {
            var saved = reservationRepository.save(reservation);
            LOGGER.info("reservation.created reservationId={} offerId={}", saved.getId(), offer.offerId());
            return ReservationResult.created(saved);
        } catch (ActiveReservationAlreadyExistsException concurrentInsert) {
            LOGGER.info("reservation.reused offerId={} reason=concurrent-request", offer.offerId());
            return reservationRepository.findActiveByConsumerAndOffer(command.consumerId(), command.offerId())
                    .map(ReservationResult::existing)
                    .orElseThrow(() -> concurrentInsert);
        }
    }

    private ReservationCode newCode() {
        return Stream.generate(() -> ReservationCode.generate(random))
                .limit(MAX_CODE_ATTEMPTS)
                .filter(code -> !reservationRepository.existsByCode(code.value()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(CODES_EXHAUSTED_MESSAGE.formatted(MAX_CODE_ATTEMPTS)));
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }

    private static LocalDateTime endOfValidity(OfferSnapshot offer) {
        return offer.validTo().atTime(END_OF_DAY).atZone(OFFER_ZONE)
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
