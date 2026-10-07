@US23 @identity
Feature: Log in as business
  As a business owner, I want to log in to manage my advertising campaigns.

  Background:
    Given the business "Bodega Doña Rosa" owned by "rosa.quispe@ejemplo.pe" exists with password "Bodega#2026"

  Scenario: Valid business credentials
    When they log in with "rosa.quispe@ejemplo.pe" and "Bodega#2026"
    Then the response has status 200
    And the field "role" is "BUSINESS_OWNER"
    And the field "businessId" is the id of the business "Bodega Doña Rosa"
    And the token carries the claim "businessId" with the id of the business "Bodega Doña Rosa"
    And the token carries the claim "roles" with "ROLE_BUSINESS_OWNER"
    And the token carries the claim "sub" with the id of the account "rosa.quispe@ejemplo.pe"
    And the token carries the claim "iss" with "geops-identity"
    And the token carries the claim "aud" with "geops-api"
    And the token carries only the claims "sub, iss, aud, roles, businessId, iat, exp"
    And the token does not carry the claims "consumerId, jti"
    And the token expires one hour after it is issued
    And the token signature is validated with the key published at "/.well-known/jwks.json"
