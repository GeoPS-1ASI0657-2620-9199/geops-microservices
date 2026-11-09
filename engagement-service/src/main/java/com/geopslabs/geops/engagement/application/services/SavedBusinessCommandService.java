package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.application.usecases.*;
import com.geopslabs.geops.engagement.domain.models.*;
import com.geopslabs.geops.engagement.domain.models.exceptions.*;
import com.geopslabs.geops.engagement.domain.ports.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.List;

public class SavedBusinessCommandService implements SaveBusinessUseCase, RemoveSavedBusinessUseCase, ListSavedBusinessesUseCase {
    private final SavedBusinessRepositoryPort saved;
    private final BusinessSnapshotRepositoryPort businesses;
    private final Clock clock;
    public SavedBusinessCommandService(SavedBusinessRepositoryPort saved, BusinessSnapshotRepositoryPort businesses, Clock clock) {
        this.saved = saved; this.businesses = businesses; this.clock = clock;
    }

    @Override
    @Transactional
    public Result save(Long consumerId, Long businessId) {
        businesses.findById(businessId).orElseThrow(BusinessNotFoundException::new);
        var inserted = saved.saveIfAbsent(consumerId, businessId, clock.instant());
        return new Result(inserted.orElseGet(() -> saved.findByConsumerAndBusiness(consumerId, businessId).orElseThrow()), inserted.isPresent());
    }

    @Override
    @Transactional
    public void remove(Long consumerId, Long businessId) {
        if (saved.delete(consumerId, businessId) == 0) throw new SavedBusinessNotFoundException();
    }

    @Override
    public List<SavedBusiness> list(Long consumerId) { return saved.findByConsumer(consumerId); }
}
