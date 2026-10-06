package com.geopslabs.geops.identity.infrastructure.web;

import com.geopslabs.geops.identity.application.usecases.LogInCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogInRequest(
        @NotBlank @Email @Size(max = LogInRequest.EMAIL_MAX_LENGTH) String email,
        @NotBlank @Size(max = LogInRequest.PASSWORD_MAX_LENGTH) String password) {

    static final int EMAIL_MAX_LENGTH = 255;
    static final int PASSWORD_MAX_LENGTH = 72;

    public LogInCommand toCommand() {
        return new LogInCommand(email, password);
    }
}
