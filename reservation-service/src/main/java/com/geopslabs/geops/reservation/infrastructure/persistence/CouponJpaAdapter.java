package com.geopslabs.geops.reservation.infrastructure.persistence;

import com.geopslabs.geops.reservation.domain.models.Coupon;
import com.geopslabs.geops.reservation.domain.ports.CouponRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CouponJpaAdapter implements CouponRepositoryPort {
    private final CouponJpaRepository repository;

    public CouponJpaAdapter(CouponJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Coupon save(Coupon coupon) {
        var entity = coupon.getId() == null ? new CouponJpaEntity()
                : repository.findById(coupon.getId()).orElseGet(CouponJpaEntity::new);
        return CouponPersistenceMapper.toDomain(repository.save(CouponPersistenceMapper.toEntity(coupon, entity)));
    }

    @Override
    public Optional<Coupon> findById(Long id) {
        return repository.findById(id).map(CouponPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Coupon> findByCode(String code) {
        return repository.findByCode(code).map(CouponPersistenceMapper::toDomain);
    }

    @Override
    public List<Coupon> findAll() {
        return toDomain(repository.findAll());
    }

    @Override
    public List<Coupon> findByUserId(Long userId) {
        return toDomain(repository.findByUser_Id(userId));
    }

    @Override
    public List<Coupon> findByPaymentId(Long paymentId) {
        return toDomain(repository.findByPayment_Id(paymentId));
    }

    @Override
    public List<Coupon> findByPaymentCode(String paymentCode) {
        return toDomain(repository.findByPaymentCode(paymentCode));
    }

    @Override
    public List<Coupon> findByOfferId(Long offerId) {
        return toDomain(repository.findByOfferId(offerId));
    }

    @Override
    public List<Coupon> findByProductType(String productType) {
        return toDomain(repository.findByProductType(productType));
    }

    @Override
    public List<Coupon> findValidCouponsByUserId(Long userId, String currentTime) {
        return toDomain(repository.findValidCouponsByUserId(userId, currentTime));
    }

    @Override
    public List<Coupon> findExpiredCoupons(String currentTime) {
        return toDomain(repository.findExpiredCoupons(currentTime));
    }

    @Override
    public long countByUserId(Long userId) {
        return repository.countByUser_Id(userId);
    }

    @Override
    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private static List<Coupon> toDomain(List<CouponJpaEntity> entities) {
        return entities.stream().map(CouponPersistenceMapper::toDomain).toList();
    }
}
