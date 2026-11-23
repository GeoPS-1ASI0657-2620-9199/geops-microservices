package com.geopslabs.geops.identity.configuration;

import com.geopslabs.geops.identity.infrastructure.sunat.SunatProperties;
import com.geopslabs.geops.identity.infrastructure.sunat.SunatRegistryLoader;
import com.geopslabs.geops.identity.infrastructure.sunat.SunatRegistrySchedule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Clock;

/**
 * The SUNAT reduced registry. With GEOPS_SUNAT_SCHEDULE_ENABLED=false nothing loads it on its own (the tests load
 * it when a scenario asks for it), and with the online lookup (GEOPS_TAXPAYER_LOOKUP=decolecta) it is not downloaded.
 */
@Configuration
@EnableConfigurationProperties(SunatProperties.class)
public class SunatConfiguration {

    @Bean
    public SunatRegistryLoader sunatRegistryLoader(DataSource dataSource,
                                                   PlatformTransactionManager transactionManager,
                                                   ResourceLoader resourceLoader, SunatProperties properties,
                                                   Clock clock) {
        return new SunatRegistryLoader(dataSource, new TransactionTemplate(transactionManager), resourceLoader,
                properties, clock);
    }

    @Bean
    @ConditionalOnExpression("${geops.sunat.schedule-enabled:true} and '${geops.taxpayer-lookup.provider:registry}' == 'registry'")
    public SunatRegistrySchedule sunatRegistrySchedule(SunatRegistryLoader loader, TaskScheduler taskScheduler,
                                                       SunatProperties properties, Clock clock) {
        return new SunatRegistrySchedule(loader, taskScheduler, properties, clock);
    }
}
