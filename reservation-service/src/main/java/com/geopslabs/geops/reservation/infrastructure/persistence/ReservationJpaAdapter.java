package com.geopslabs.geops.reservation.infrastructure.persistence;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.ReservationStatus;
import com.geopslabs.geops.reservation.domain.models.exceptions.ActiveReservationAlreadyExistsException;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ReservationJpaAdapter implements ReservationRepositoryPort {
    static final String ACTIVE_RESERVATION_INDEX = "ux_reservations_consumer_offer_active";

    private final ReservationJpaRepository repository;

    public ReservationJpaAdapter(ReservationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Reservation save(Reservation reservation) {
        try {
            var saved = repository.saveAndFlush(ReservationPersistenceMapper.toEntity(reservation));
            return ReservationPersistenceMapper.toDomain(saved);
        } catch (DataIntegrityViolationException exception) {
            throw translate(exception, reservation);
        }
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        return repository.findById(id).map(ReservationPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Reservation> findByCode(String code) {
        return repository.findByCode(code).map(ReservationPersistenceMapper::toDomain);
    }

    @Override
    public List<Reservation> findByConsumerId(Long consumerId, ReservationStatus status) {
        var entities = status == null
                ? repository.findByConsumerIdOrderByReservedAtDesc(consumerId)
                : repository.findByConsumerIdAndStatusOrderByReservedAtDesc(consumerId, status);
        return entities.stream().map(ReservationPersistenceMapper::toDomain).toList();
    }

    @Override
    public Optional<Reservation> findActiveByConsumerAndOffer(Long consumerId, Long offerId) {
        return repository.findByConsumerIdAndOfferIdAndStatus(consumerId, offerId, ReservationStatus.ACTIVE)
                .map(ReservationPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }

    private static RuntimeException translate(DataIntegrityViolationException exception, Reservation reservation) {
        var cause = NestedExceptionUtils.getMostSpecificCause(exception).getMessage();
        if (cause != null && cause.contains(ACTIVE_RESERVATION_INDEX)) {
            return new ActiveReservationAlreadyExistsException(reservation.getConsumerId(), reservation.getOfferId());
        }
        return exception;
    }
}
