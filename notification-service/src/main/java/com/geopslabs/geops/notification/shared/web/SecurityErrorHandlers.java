package com.geopslabs.geops.notification.shared.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityErrorHandlers.class);
    private static final String UNAUTHORIZED = "UNAUTHORIZED";
    private static final String UNAUTHORIZED_MESSAGE = "A valid token is required";
    private static final String FORBIDDEN = "FORBIDDEN";
    private static final String FORBIDDEN_MESSAGE = "Your account is not allowed to perform this operation";
    private static final String BEARER_CHALLENGE = "Bearer";

    private final ObjectMapper objectMapper;

    public SecurityErrorHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        LOGGER.info("request.rejected code={} path={}", UNAUTHORIZED, request.getRequestURI());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, BEARER_CHALLENGE);
        write(response, HttpStatus.UNAUTHORIZED, new ErrorResponse(UNAUTHORIZED, UNAUTHORIZED_MESSAGE));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        LOGGER.info("request.rejected code={} path={}", FORBIDDEN, request.getRequestURI());
        write(response, HttpStatus.FORBIDDEN, new ErrorResponse(FORBIDDEN, FORBIDDEN_MESSAGE));
    }

    private void write(HttpServletResponse response, HttpStatus status, ErrorResponse body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
