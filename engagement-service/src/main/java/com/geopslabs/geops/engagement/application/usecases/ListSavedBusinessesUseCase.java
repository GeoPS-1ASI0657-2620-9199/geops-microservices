package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.SavedBusiness;
import java.util.List;
public interface ListSavedBusinessesUseCase { List<SavedBusiness> list(Long consumerId); }
