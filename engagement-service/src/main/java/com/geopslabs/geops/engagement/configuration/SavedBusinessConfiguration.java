package com.geopslabs.geops.engagement.configuration;

import com.geopslabs.geops.engagement.application.services.SavedBusinessCommandService;
import com.geopslabs.geops.engagement.domain.ports.*;
import org.springframework.context.annotation.*;
import java.time.Clock;
@Configuration
public class SavedBusinessConfiguration {
    @Bean
    SavedBusinessCommandService savedBusinessCommandService(SavedBusinessRepositoryPort saved, BusinessSnapshotRepositoryPort businesses, Clock clock) { return new SavedBusinessCommandService(saved, businesses, clock); }
}
