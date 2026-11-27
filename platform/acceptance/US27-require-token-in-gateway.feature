@US27 @platform @security
Feature: Require a valid token in the API gateway
  As the technical owner, I want the API gateway to reject every request without a valid token
  before it reaches the services, so that no endpoint is exposed by omission.

  Scenario: The public routes answer without a token
    When an anonymous user searches nearby offers, reads the offer categories, the JWKS, the API docs and the home page
    Then every one answers 200
    And the login and the email unsubscription link reach their service without a token

  Scenario: Every protected route rejects a request without a token
    When an anonymous user calls one route of each protected block of the gateway, a write on the offers route and the logout
    Then every one answers 401 with code "UNAUTHORIZED"
    And the gateway log shows that none of them reached a service

  Scenario: A malformed token is rejected by the gateway
    When a request with a malformed token reads the reservations
    Then the response has status 401

  Scenario: A valid token reaches the service
    Given a consumer logs in
    When they read their consumer profile through the gateway
    Then the response has status 200

  Scenario: The validation adds at most 50 ms to the p95
    Given a consumer logs in
    When they read their consumer profile 200 times through the gateway and 200 times straight from identity
    Then the p95 through the gateway exceeds the direct p95 by 50 ms at most

  Scenario: A closed session stops passing the gateway
    Given a consumer logs in
    When they log out
    Then within 6 seconds the gateway rejects their token

  Scenario: The token validation is not published
    When a request calls POST /api/v1/auth/introspect through the gateway
    Then the response has status 404

  Scenario: Only the production web and the local web may call the API from a browser
    When a browser preflight from "https://geops-web-app.vercel.app" and one from "http://localhost:4200" reach the gateway
    Then both answers allow their own origin
    And a preflight from "https://another-project.vercel.app" is not allowed

  Scenario: The API docs are rate limited like the API
    When an anonymous user requests the catalog API docs 80 times at once
    Then at least one answer has status 429
    And the catalog API docs answer 200 again after 2 seconds
