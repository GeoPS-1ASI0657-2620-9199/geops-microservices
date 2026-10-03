package com.geopslabs.geops.identity.infrastructure.web;

import com.geopslabs.geops.identity.application.usecases.RegisterUserCommand;
import com.geopslabs.geops.identity.domain.models.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotNull Role role,
        @NotBlank @Size(max = RegisterUserRequest.FULL_NAME_MAX_LENGTH) String fullName,
        @NotBlank @Email @Size(max = RegisterUserRequest.EMAIL_MAX_LENGTH) String email,
        @NotBlank @Pattern(regexp = RegisterUserRequest.PHONE_PATTERN) String phone,
        @NotBlank @Size(min = RegisterUserRequest.PASSWORD_MIN_LENGTH,
                max = RegisterUserRequest.PASSWORD_MAX_LENGTH) String password) {

    static final int FULL_NAME_MAX_LENGTH = 255;
    static final int EMAIL_MAX_LENGTH = 255;
    static final String PHONE_PATTERN = "^9\\d{8}$";
    static final int PASSWORD_MIN_LENGTH = 8;
    static final int PASSWORD_MAX_LENGTH = 72;

    public RegisterUserCommand toCommand() {
        return new RegisterUserCommand(role, fullName.strip(), email, phone, password);
    }
}
