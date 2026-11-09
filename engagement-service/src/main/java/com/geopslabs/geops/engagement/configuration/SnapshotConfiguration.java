package com.geopslabs.geops.engagement.configuration;

import com.geopslabs.geops.engagement.application.services.SnapshotProjectorService;
import com.geopslabs.geops.engagement.domain.ports.SnapshotProjectionPort;
import org.springframework.context.annotation.*;
@Configuration
public class SnapshotConfiguration {
    @Bean
    SnapshotProjectorService snapshotProjectorService(SnapshotProjectionPort projections) { return new SnapshotProjectorService(projections); }
}
