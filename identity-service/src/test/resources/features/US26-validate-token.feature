@US26 @identity @security
Feature: Validate the token through the identity service API
  As the technical owner, I want the identity service to publish a token validation operation
  so that each service checks the authentication without querying the identity database.

  Scenario: A valid token gives the identity of the requester and its validity
    Given a consumer "lucia.fernandez@ejemplo.pe" exists with password "Ofertas#2026"
    And they log in with "lucia.fernandez@ejemplo.pe" and "Ofertas#2026"
    When the token they received is validated
    Then the response has status 200
    And the field "active" is "true"
    And the field "role" is "CONSUMER"
    And the field "userId" is the id of the account "lucia.fernandez@ejemplo.pe"
    And the validity ends one hour after the token was issued

  Scenario: A business owner token also gives the business
    Given the business "Bodega Doña Rosa" owned by "rosa.quispe@ejemplo.pe" exists with password "Bodega#2026"
    And they log in with "rosa.quispe@ejemplo.pe" and "Bodega#2026"
    When the token they received is validated
    Then the response has status 200
    And the field "role" is "BUSINESS_OWNER"
    And the field "businessId" is the id of the business "Bodega Doña Rosa"

  Scenario Outline: An expired or altered token is rejected without saying why
    When a token that is <kind> is validated
    Then the response has status 401
    And the field "active" is "false"
    And the response does not contain "userId"

    Examples:
      | kind                      |
      | expired                   |
      | altered after signing     |
      | signed with another key   |
      | issued for another system |
      | malformed                 |
      | missing                   |

  Scenario: A token whose session was closed is reported inactive
    Given a consumer "lucia.fernandez@ejemplo.pe" exists with password "Ofertas#2026"
    And they log in with "lucia.fernandez@ejemplo.pe" and "Ofertas#2026"
    When they log out with the token they received
    Then the response has status 204
    When the token they received is validated
    Then the response has status 401
    And the field "active" is "false"
    When they log out with the token they received
    Then the response has status 204

  Scenario: Closing a session needs a valid token
    When a token that is expired is used to log out
    Then the response has status 401
    And the field "code" is "INVALID_TOKEN"
    And the field "message" is "Tu sesión no es válida o ya terminó. Inicia sesión de nuevo."

  Scenario: The validation does not read the accounts
    When a valid token of the account 9001, which is not stored, is validated
    Then the response has status 200
    And the field "userId" is "9001"
