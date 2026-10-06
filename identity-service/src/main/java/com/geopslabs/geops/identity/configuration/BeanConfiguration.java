package com.geopslabs.geops.identity.configuration;

import com.geopslabs.geops.identity.application.services.PublicKeyQueryService;
import com.geopslabs.geops.identity.application.services.UserCommandService;
import com.geopslabs.geops.identity.application.services.UserAuthenticationService;
import com.geopslabs.geops.identity.application.usecases.GetPublicKeysUseCase;
import com.geopslabs.geops.identity.application.usecases.LogInUseCase;
import com.geopslabs.geops.identity.application.usecases.RegisterUserUseCase;
import com.geopslabs.geops.identity.domain.ports.BusinessProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.ConsumerProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.PasswordHasherPort;
import com.geopslabs.geops.identity.domain.ports.TokenIssuerPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

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
    public LogInUseCase logInUseCase(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher,
                                     TokenIssuerPort tokenIssuer, Clock clock) {
        return new UserAuthenticationService(userRepository, passwordHasher, tokenIssuer, clock);
    }

    @Bean
    public GetPublicKeysUseCase getPublicKeysUseCase(TokenIssuerPort tokenIssuer) {
        return new PublicKeyQueryService(tokenIssuer);
    }
}
