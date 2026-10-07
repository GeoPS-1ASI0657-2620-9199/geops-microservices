package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.application.usecases.BusinessProfileData;
import com.geopslabs.geops.identity.application.usecases.RegisterUserCommand;
import com.geopslabs.geops.identity.domain.models.AccountStatus;
import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.models.BusinessProfileNotAllowedException;
import com.geopslabs.geops.identity.domain.models.BusinessProfileRequiredException;
import com.geopslabs.geops.identity.domain.models.ConsumerProfile;
import com.geopslabs.geops.identity.domain.models.EmailAlreadyRegisteredException;
import com.geopslabs.geops.identity.domain.models.InvalidLocationException;
import com.geopslabs.geops.identity.domain.models.InvalidRucException;
import com.geopslabs.geops.identity.domain.models.PhoneAlreadyRegisteredException;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.RoleNotAllowedException;
import com.geopslabs.geops.identity.domain.models.Ruc;
import com.geopslabs.geops.identity.domain.models.RucAlreadyRegisteredException;
import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.models.VerificationStatus;
import com.geopslabs.geops.identity.domain.ports.BusinessProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.ConsumerProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.PasswordHasherPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {
    private static final Long USER_ID = 41L;
    private static final Long PROFILE_ID = 15L;
    private static final int NO_FAILED_ATTEMPTS = 0;
    private static final int DEFAULT_SEARCH_RADIUS_MINUTES = 10;
    private static final String FULL_NAME = "Lucía Fernández Ríos";
    private static final String EMAIL = "lucia.fernandez@ejemplo.pe";
    private static final String PHONE = "987123456";
    private static final String PASSWORD = "Ofertas#2026";
    private static final String PASSWORD_HASH = "$2a$10$hash";
    private static final Long BUSINESS_PROFILE_ID = 7L;
    private static final String OWNER_NAME = "Rosa Quispe Mamani";
    private static final String OWNER_EMAIL = "rosa.quispe@ejemplo.pe";
    private static final String OWNER_PHONE = "987654321";
    private static final String OWNER_PASSWORD = "Bodega#2026";
    private static final String BUSINESS_NAME = "Bodega Doña Rosa";
    private static final String RUC = "10456789019";
    private static final String ADDRESS = "Jr. Huánuco 1250, La Victoria";
    private static final double LATITUDE = -12.0681;
    private static final double LONGITUDE = -77.0350;
    private static final double OUT_OF_RANGE_LATITUDE = 95.0;

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private ConsumerProfileRepositoryPort consumerProfileRepository;
    @Mock
    private BusinessProfileRepositoryPort businessProfileRepository;
    @Mock
    private PasswordHasherPort passwordHasher;

    private UserCommandService service;

    @BeforeEach
    void setUp() {
        service = new UserCommandService(userRepository, consumerProfileRepository, businessProfileRepository,
                passwordHasher);
    }

    @Test
    void registersConsumerWithItsProfile() {
        givenSavesSucceed();

        var registered = service.register(command(Role.CONSUMER, EMAIL));

        assertThat(registered.userId()).isEqualTo(USER_ID);
        assertThat(registered.role()).isEqualTo(Role.CONSUMER);
        assertThat(registered.consumerProfileId()).isEqualTo(PROFILE_ID);
    }

    @Test
    void storesOnlyThePasswordHash() {
        givenSavesSucceed();
        var savedUser = ArgumentCaptor.forClass(User.class);

        service.register(command(Role.CONSUMER, EMAIL));

        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(savedUser.getValue().getFailedLoginAttempts()).isEqualTo(NO_FAILED_ATTEMPTS);
    }

    @Test
    void createsTheConsumerProfileWithDefaultValues() {
        givenSavesSucceed();
        var savedProfile = ArgumentCaptor.forClass(ConsumerProfile.class);

        service.register(command(Role.CONSUMER, EMAIL));

        verify(consumerProfileRepository).save(savedProfile.capture());
        assertThat(savedProfile.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(savedProfile.getValue().isLocationPermission()).isFalse();
        assertThat(savedProfile.getValue().getSearchRadiusMinutes()).isEqualTo(DEFAULT_SEARCH_RADIUS_MINUTES);
    }

    @Test
    void normalizesTheEmailBeforeCheckingAndSaving() {
        givenSavesSucceed();

        var registered = service.register(command(Role.CONSUMER, "  Lucia.Fernandez@Ejemplo.PE "));

        verify(userRepository).existsByEmail(EMAIL);
        assertThat(registered.email()).isEqualTo(EMAIL);
    }

    @Test
    void rejectsDuplicatedEmail() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> service.register(command(Role.CONSUMER, EMAIL)))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessageNotContaining(FULL_NAME);
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicatedPhone() {
        when(userRepository.existsByPhone(PHONE)).thenReturn(true);

        assertThatThrownBy(() -> service.register(command(Role.CONSUMER, EMAIL)))
                .isInstanceOf(PhoneAlreadyRegisteredException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsAdminRole() {
        assertThatThrownBy(() -> service.register(command(Role.ADMIN, EMAIL)))
                .isInstanceOf(RoleNotAllowedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registersBusinessOwnerWithItsBusinessProfile() {
        givenBusinessSavesSucceed();

        var registered = service.register(businessCommand(business(RUC, ADDRESS, LATITUDE)));

        assertThat(registered.role()).isEqualTo(Role.BUSINESS_OWNER);
        assertThat(registered.businessProfileId()).isEqualTo(BUSINESS_PROFILE_ID);
        assertThat(registered.consumerProfileId()).isNull();
        assertThat(registered.accountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(registered.verificationStatus()).isEqualTo(VerificationStatus.UNVERIFIED);
        verify(consumerProfileRepository, never()).save(any());
    }

    @Test
    void storesTheBusinessProfileForTheNewUser() {
        givenBusinessSavesSucceed();
        var savedProfile = ArgumentCaptor.forClass(BusinessProfile.class);

        service.register(businessCommand(business(RUC, ADDRESS, LATITUDE)));

        verify(businessProfileRepository).save(savedProfile.capture());
        assertThat(savedProfile.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(savedProfile.getValue().getRuc()).isEqualTo(new Ruc(RUC));
        assertThat(savedProfile.getValue().getLocation().latitude()).isEqualTo(LATITUDE);
        assertThat(savedProfile.getValue().getLocation().longitude()).isEqualTo(LONGITUDE);
    }

    @Test
    void requiresBusinessProfileForBusinessOwner() {
        assertThatThrownBy(() -> service.register(businessCommand(null)))
                .isInstanceOf(BusinessProfileRequiredException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsBusinessProfileForConsumer() {
        var command = new RegisterUserCommand(Role.CONSUMER, FULL_NAME, EMAIL, PHONE, PASSWORD,
                business(RUC, ADDRESS, LATITUDE));

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(BusinessProfileNotAllowedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsOutOfRangeLocationBeforeSavingAnything() {
        assertThatThrownBy(() -> service.register(businessCommand(business(RUC, ADDRESS, OUT_OF_RANGE_LATITUDE))))
                .isInstanceOf(InvalidLocationException.class)
                .hasMessageContaining("latitud");
        verify(userRepository, never()).save(any());
        verify(businessProfileRepository, never()).save(any());
    }

    @Test
    void rejectsBlankAddressAsInvalidLocation() {
        assertThatThrownBy(() -> service.register(businessCommand(business(RUC, " ", LATITUDE))))
                .isInstanceOf(InvalidLocationException.class)
                .hasMessageContaining("dirección");
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsMalformedRuc() {
        assertThatThrownBy(() -> service.register(businessCommand(business("1045678901", ADDRESS, LATITUDE))))
                .isInstanceOf(InvalidRucException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsRucAlreadyRegistered() {
        when(businessProfileRepository.existsByRuc(new Ruc(RUC))).thenReturn(true);

        assertThatThrownBy(() -> service.register(businessCommand(business(RUC, ADDRESS, LATITUDE))))
                .isInstanceOf(RucAlreadyRegisteredException.class);
        verify(userRepository, never()).save(any());
        verify(businessProfileRepository, never()).save(any());
    }

    private void givenSavesSucceed() {
        when(passwordHasher.encode(PASSWORD)).thenReturn(PASSWORD_HASH);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(consumerProfileRepository.save(any(ConsumerProfile.class)))
                .thenAnswer(invocation -> new ConsumerProfile(PROFILE_ID, invocation.getArgument(0)));
    }

    private void givenBusinessSavesSucceed() {
        when(passwordHasher.encode(OWNER_PASSWORD)).thenReturn(PASSWORD_HASH);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(businessProfileRepository.save(any(BusinessProfile.class))).thenAnswer(invocation -> {
            BusinessProfile profile = invocation.getArgument(0);
            return new BusinessProfile(BUSINESS_PROFILE_ID, profile.getUserId(), profile,
                    profile.getAccountStatus(), profile.getVerificationStatus());
        });
    }

    private static BusinessProfileData business(String ruc, String address, double latitude) {
        return new BusinessProfileData(BUSINESS_NAME, "Bodega", ruc, address, latitude, LONGITUDE,
                "Lun-Sáb 07:00-22:00");
    }

    private static RegisterUserCommand businessCommand(BusinessProfileData businessProfile) {
        return new RegisterUserCommand(Role.BUSINESS_OWNER, OWNER_NAME, OWNER_EMAIL, OWNER_PHONE, OWNER_PASSWORD,
                businessProfile);
    }

    private static User withId(User user) {
        return new User(USER_ID, user, null, user.getFailedLoginAttempts(), null, Instant.now());
    }

    private static RegisterUserCommand command(Role role, String email) {
        return new RegisterUserCommand(role, FULL_NAME, email, PHONE, PASSWORD, null);
    }
}
