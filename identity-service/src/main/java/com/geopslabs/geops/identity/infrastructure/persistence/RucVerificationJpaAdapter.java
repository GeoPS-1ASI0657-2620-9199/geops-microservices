package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.domain.models.RucVerification;
import com.geopslabs.geops.identity.domain.models.Ruc;
import com.geopslabs.geops.identity.domain.models.VerificationReason;
import com.geopslabs.geops.identity.domain.ports.RucVerificationRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RucVerificationJpaAdapter implements RucVerificationRepositoryPort {
    private final RucVerificationJpaRepository repository;

    public RucVerificationJpaAdapter(RucVerificationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public RucVerification save(RucVerification verification) {
        var entity = new RucVerificationJpaEntity();
        entity.setId(verification.id());
        entity.setBusinessProfileId(verification.businessProfileId());
        entity.setRuc(verification.ruc().number());
        entity.setTaxpayerStatus(verification.taxpayerStatus());
        entity.setTaxpayerCondition(verification.taxpayerCondition());
        entity.setResult(verification.result());
        entity.setRejectionReason(verification.reason() == null ? null : verification.reason().name());
        entity.setCheckedAt(verification.checkedAt());
        return verification.withId(repository.save(entity).getId());
    }

    @Override
    public Optional<RucVerification> findLatestByBusinessProfileId(Long businessProfileId) {
        return repository.findFirstByBusinessProfileIdOrderByCheckedAtDescIdDesc(businessProfileId)
                .map(RucVerificationJpaAdapter::toDomain);
    }

    private static RucVerification toDomain(RucVerificationJpaEntity entity) {
        var reason = entity.getRejectionReason() == null ? null : VerificationReason.valueOf(entity.getRejectionReason());
        return new RucVerification(entity.getId(), entity.getBusinessProfileId(), new Ruc(entity.getRuc()),
                entity.getResult(), reason, entity.getTaxpayerStatus(), entity.getTaxpayerCondition(),
                entity.getCheckedAt());
    }
}
