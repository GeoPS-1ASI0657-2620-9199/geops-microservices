@identity @security
Feature: Profile routes need a token of the right role
  Identity validates its own tokens on the profile routes and keeps registration and login public.

  Scenario: A profile route without a token is rejected
    When an anonymous user reads the business profile 1
    Then the response has status 401
    And the field "code" is "UNAUTHORIZED"

  Scenario: A malformed token is rejected
    When a request with a malformed token reads the business profile 1
    Then the response has status 401

  Scenario: A consumer token cannot reach the business profile routes
    When a consumer token reads the business profile 1
    Then the response has status 403
    And the field "code" is "FORBIDDEN"

  Scenario: A business owner token cannot reach the consumer profile routes
    When a business owner token reads the consumer profile
    Then the response has status 403

  Scenario: Login ignores a stale token sent by the browser
    Given a consumer "lucia.fernandez@ejemplo.pe" exists with password "Ofertas#2026"
    When they log in with "lucia.fernandez@ejemplo.pe" and "Ofertas#2026" while the browser still sends an invalid token
    Then the response has status 200
