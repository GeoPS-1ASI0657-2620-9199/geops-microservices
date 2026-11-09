package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.queries.GetFollowedBusinessesQuery;
import java.util.List;

public interface ListFollowedBusinessesUseCase {
    List<FollowedBusinessView> list(GetFollowedBusinessesQuery query);
}
