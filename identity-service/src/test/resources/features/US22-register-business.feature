@US22 @identity
Feature: Register business
  As a business owner, I want to register my store to publish campaigns aimed at nearby customers.

  Scenario: The business registers with its data and location
    When they register the business "Bodega Doña Rosa" with RUC "10456789019", address "Jr. Huánuco 1250, La Victoria", latitude -12.0681 and longitude -77.0350
    Then the response has status 201
    And the field "role" is "BUSINESS_OWNER"
    And the field "verificationStatus" is "UNVERIFIED"
    And the account "rosa.quispe@ejemplo.pe" has a business profile

  Scenario Outline: The location is invalid
    When they register the business "Bodega Doña Rosa" with RUC "10456789019", address "<address>", latitude <latitude> and longitude <longitude>
    Then the response has status 400
    And the field "code" is "INVALID_LOCATION"

    Examples:
      | address                        | latitude | longitude |
      | Jr. Huánuco 1250, La Victoria  | 95.0     | -77.0350  |
      | Jr. Huánuco 1250, La Victoria  | -12.0681 | -200.0    |
      |                                | -12.0681 | -77.0350  |
