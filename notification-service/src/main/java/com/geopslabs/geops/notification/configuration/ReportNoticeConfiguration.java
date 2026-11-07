package com.geopslabs.geops.notification.configuration;

import com.geopslabs.geops.notification.application.services.ReportNoticeService;
import com.geopslabs.geops.notification.application.ports.NotificationChannelStrategy;
import com.geopslabs.geops.notification.domain.ports.NoticePlanningRepositoryPort;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.util.List;
@Configuration
public class ReportNoticeConfiguration {
    @Bean
    ReportNoticeService reportNoticeService(NoticePlanningRepositoryPort notices, List<NotificationChannelStrategy> channels, PlatformTransactionManager manager, Clock clock) { return new ReportNoticeService(notices, channels, new TransactionTemplate(manager), clock); }
}
