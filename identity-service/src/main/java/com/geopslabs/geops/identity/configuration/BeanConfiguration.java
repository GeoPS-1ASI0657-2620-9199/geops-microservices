package com.geopslabs.geops.identity.configuration;

import com.geopslabs.geops.identity.application.services.UserCommandService;
import com.geopslabs.geops.identity.application.services.UserQueryService;
import com.geopslabs.geops.identity.application.usecases.UserCommandUseCase;
import com.geopslabs.geops.identity.application.usecases.UserQueryUseCase;
import com.geopslabs.geops.identity.domain.ports.PasswordHasherPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public UserCommandUseCase userCommandUseCase(UserRepositoryPort userRepository,
                                                 PasswordHasherPort passwordHasher) {
        return new UserCommandService(userRepository, passwordHasher);
    }

    @Bean
    public UserQueryUseCase userQueryUseCase(UserRepositoryPort userRepository) {
        return new UserQueryService(userRepository);
    }
}
