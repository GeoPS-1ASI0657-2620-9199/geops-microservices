@US20 @identity
Feature: Register consumer account
  As a consumer, I want to sign up on the platform to access the offers in my area.

  Scenario: A new user signs up with valid data
    Given no account exists with the email "lucia.fernandez@ejemplo.pe"
    When they register as "CONSUMER" with name "Lucía Fernández Ríos", email "lucia.fernandez@ejemplo.pe", phone "987123456" and password "Ofertas#2026"
    Then the response has status 201
    And the field "role" is "CONSUMER"
    And the account "lucia.fernandez@ejemplo.pe" has a consumer profile

  Scenario: The email is already registered
    Given an account for "Lucía Fernández Ríos" exists with the email "lucia.fernandez@ejemplo.pe"
    When they register as "CONSUMER" with name "Mario Salas Paz", email "lucia.fernandez@ejemplo.pe", phone "987000111" and password "Ofertas#2026"
    Then the response has status 409
    And the field "code" is "EMAIL_ALREADY_REGISTERED"
    And the response does not contain "Lucía Fernández Ríos"
