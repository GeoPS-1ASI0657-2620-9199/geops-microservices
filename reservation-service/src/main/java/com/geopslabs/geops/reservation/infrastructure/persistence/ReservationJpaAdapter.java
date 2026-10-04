package com.geopslabs.geops.reservation.infrastructure.persistence;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ReservationJpaAdapter implements ReservationRepositoryPort {
    private final ReservationJpaRepository repository;

    public ReservationJpaAdapter(ReservationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Reservation save(Reservation reservation) {
        var entity = reservation.getId() == null ? new ReservationJpaEntity()
                : repository.findById(reservation.getId()).orElseGet(ReservationJpaEntity::new);
        return ReservationPersistenceMapper.toDomain(repository.save(ReservationPersistenceMapper.toEntity(reservation, entity)));
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
    public List<Reservation> findAll() {
        return toDomain(repository.findAll());
    }

    @Override
    public List<Reservation> findByConsumerId(Long consumerId) {
        return toDomain(repository.findByConsumerId(consumerId));
    }

    @Override
    public List<Reservation> findByPaymentId(Long paymentId) {
        return toDomain(repository.findByPaymentId(paymentId));
    }

    @Override
    public List<Reservation> findByPaymentCode(String paymentCode) {
        return toDomain(repository.findByPaymentCode(paymentCode));
    }

    @Override
    public List<Reservation> findByOfferId(Long offerId) {
        return toDomain(repository.findByOfferId(offerId));
    }

    @Override
    public List<Reservation> findByProductType(String productType) {
        return toDomain(repository.findByProductType(productType));
    }

    @Override
    public List<Reservation> findValidReservationsByConsumerId(Long consumerId, String currentTime) {
        return toDomain(repository.findValidReservationsByConsumerId(consumerId, currentTime));
    }

    @Override
    public List<Reservation> findExpiredReservations(String currentTime) {
        return toDomain(repository.findExpiredReservations(currentTime));
    }

    @Override
    public long countByConsumerId(Long consumerId) {
        return repository.countByConsumerId(consumerId);
    }

    @Override
    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private static List<Reservation> toDomain(List<ReservationJpaEntity> entities) {
        return entities.stream().map(ReservationPersistenceMapper::toDomain).toList();
    }
}
