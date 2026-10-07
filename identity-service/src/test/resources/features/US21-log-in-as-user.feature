@US21 @identity
Feature: Log in as user
  As a consumer, I want to log in to access my saved offers and preferences.

  Background:
    Given a consumer "lucia.fernandez@ejemplo.pe" exists with password "Ofertas#2026"

  Scenario: Valid credentials
    When they log in with "lucia.fernandez@ejemplo.pe" and "Ofertas#2026"
    Then the response has status 200
    And the field "role" is "CONSUMER"
    And the token signature is validated with the key published at "/.well-known/jwks.json"

  Scenario Outline: Invalid credentials
    When they log in with "<email>" and "<password>"
    Then the response has status 401
    And the field "code" is "INVALID_CREDENTIALS"
    And the field "message" is "No se pudo iniciar sesión con esos datos."

    Examples:
      | email                       | password     |
      | lucia.fernandez@ejemplo.pe  | Otra#2026    |
      | nadie@ejemplo.pe            | Ofertas#2026 |
