package com.geopslabs.geops.engagement.configuration;

import com.geopslabs.geops.engagement.application.services.ReviewReplyCommandService;
import com.geopslabs.geops.engagement.domain.ports.ReviewRepositoryPort;
import org.springframework.context.annotation.*;
import java.time.Clock;

@Configuration
public class ReviewReplyConfiguration {
    @Bean
    ReviewReplyCommandService reviewReplyCommandService(ReviewRepositoryPort reviews, Clock clock) {
        return new ReviewReplyCommandService(reviews, clock);
    }
}
