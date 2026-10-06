package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.Favorite;
import com.geopslabs.geops.engagement.domain.ports.FavoriteRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class FavoriteJpaAdapter implements FavoriteRepositoryPort {
    private final FavoriteJpaRepository repository;

    public FavoriteJpaAdapter(FavoriteJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Favorite save(Favorite favorite) {
        var entity = favorite.getId() == null ? new FavoriteJpaEntity()
                : repository.findById(favorite.getId()).orElseGet(FavoriteJpaEntity::new);
        return FavoritePersistenceMapper.toDomain(
                repository.save(FavoritePersistenceMapper.toEntity(favorite, entity)));
    }

    @Override
    public Optional<Favorite> findById(Long id) {
        return repository.findById(id).map(FavoritePersistenceMapper::toDomain);
    }

    @Override
    public List<Favorite> findByUserId(Long userId) {
        return repository.findByUser_Id(userId).stream().map(FavoritePersistenceMapper::toDomain).toList();
    }

    @Override
    public Optional<Favorite> findByUserIdAndOfferId(Long userId, Long offerId) {
        return repository.findByUser_IdAndOffer_Id(userId, offerId).map(FavoritePersistenceMapper::toDomain);
    }

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existsByUserIdAndOfferId(Long userId, Long offerId) {
        return repository.existsByUser_IdAndOffer_Id(userId, offerId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public long deleteByUserIdAndOfferId(Long userId, Long offerId) {
        return repository.deleteByUser_IdAndOffer_Id(userId, offerId);
    }
}
