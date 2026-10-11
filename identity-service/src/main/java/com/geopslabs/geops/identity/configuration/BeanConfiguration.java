package com.geopslabs.geops.identity.configuration;

import com.geopslabs.geops.identity.application.services.BusinessProfileQueryService;
import com.geopslabs.geops.identity.application.services.BusinessVerificationService;
import com.geopslabs.geops.identity.application.services.PublicKeyQueryService;
import com.geopslabs.geops.identity.application.services.UserCommandService;
import com.geopslabs.geops.identity.application.services.UserAuthenticationService;
import com.geopslabs.geops.identity.application.usecases.GetPublicKeysUseCase;
import com.geopslabs.geops.identity.application.usecases.LogInUseCase;
import com.geopslabs.geops.identity.application.usecases.RegisterUserUseCase;
import com.geopslabs.geops.identity.domain.ports.BusinessProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.ConsumerProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.GeocodingPort;
import com.geopslabs.geops.identity.domain.ports.OutboxPort;
import com.geopslabs.geops.identity.domain.ports.PasswordHasherPort;
import com.geopslabs.geops.identity.domain.ports.RucVerificationRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.TaxpayerRegistryPort;
import com.geopslabs.geops.identity.domain.ports.TokenIssuerPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.UUID;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class BeanConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public RegisterUserUseCase registerUserUseCase(UserRepositoryPort userRepository,
                                                   ConsumerProfileRepositoryPort consumerProfileRepository,
                                                   BusinessProfileRepositoryPort businessProfileRepository,
                                                   PasswordHasherPort passwordHasher) {
        return new UserCommandService(userRepository, consumerProfileRepository, businessProfileRepository,
                passwordHasher);
    }

    @Bean
    public LogInUseCase logInUseCase(UserRepositoryPort userRepository,
                                     BusinessProfileRepositoryPort businessProfileRepository,
                                     PasswordHasherPort passwordHasher, TokenIssuerPort tokenIssuer, Clock clock) {
        return new UserAuthenticationService(userRepository, businessProfileRepository, passwordHasher, tokenIssuer,
                clock);
    }

    @Bean
    public GetPublicKeysUseCase getPublicKeysUseCase(TokenIssuerPort tokenIssuer) {
        return new PublicKeyQueryService(tokenIssuer);
    }

    @Bean
    public BusinessVerificationService businessVerificationService(BusinessProfileRepositoryPort businessProfiles,
                                                                   RucVerificationRepositoryPort verifications,
                                                                   TaxpayerRegistryPort taxpayerRegistry,
                                                                   GeocodingPort geocoding, OutboxPort outbox,
                                                                   Clock clock) {
        return new BusinessVerificationService(businessProfiles, verifications, taxpayerRegistry, geocoding, outbox,
                clock, UUID::randomUUID);
    }

    @Bean
    public BusinessProfileQueryService businessProfileQueryService(BusinessProfileRepositoryPort businessProfiles,
                                                                   RucVerificationRepositoryPort verifications) {
        return new BusinessProfileQueryService(businessProfiles, verifications);
    }
}
