@US12 @GEO-209 @engagement
Feature: Save offers
  As a consumer, I want to save offers and businesses so that I can find them again later.

  Background:
    Given business 1 is called "Bodega Doña Rosa"
    And offer 1 of business 1 titled "Menú ejecutivo a mitad de precio" is published until "2026-10-31"

  Scenario: The consumer saves a valid offer and it appears in the saved offers
    When consumer 1 saves offer 1
    Then the response has status 201
    And the field "title" is "Menú ejecutivo a mitad de precio"
    And the field "businessName" is "Bodega Doña Rosa"
    And the field "expired" is "false"
    And the field "savedAt" is "2026-10-08T13:05:00Z"
    And consumer 1 has 1 saved offer

  Scenario: Saving the same offer twice returns the same saved offer
    Given consumer 1 saved offer 1
    When consumer 1 saves offer 1
    Then the response has status 200
    And the response is the same saved offer as before
    And consumer 1 has 1 saved offer

  Scenario: An offer without a local copy cannot be saved
    When consumer 1 saves offer 999
    Then the response has status 404
    And the field "code" is "OFFER_NOT_FOUND"
    And consumer 1 has 0 saved offers

  Scenario: An offer that already expired cannot be saved
    Given offer 2 of business 1 titled "Desayuno criollo a S/ 8" is published until "2026-10-01"
    When consumer 1 saves offer 2
    Then the response has status 409
    And the field "code" is "OFFER_NOT_AVAILABLE"
    And consumer 1 has 0 saved offers

  Scenario: The listing shows only the saved offers of the consumer
    Given offer 3 of business 1 titled "Lomo saltado a S/ 15" is published until "2026-10-31"
    And consumer 1 saved offer 1
    And consumer 2 saved offer 3
    When consumer 1 lists the saved offers
    Then the response has status 200
    And the list only has offer 1

  Scenario: An offer that expired after being saved is shown as expired
    Given consumer 1 saved offer 1
    And offer 1 is now valid until "2026-10-01"
    When consumer 1 lists the saved offers
    Then the response has status 200
    And saved offer 1 has "expired" set to true

  Scenario: The consumer removes a saved offer
    Given consumer 1 saved offer 1
    When consumer 1 removes offer 1 from the saved offers
    Then the response has status 204
    And consumer 1 has 0 saved offers

  Scenario: Removing an offer that was not saved answers 404
    When consumer 1 removes offer 1 from the saved offers
    Then the response has status 404
    And the field "code" is "SAVED_OFFER_NOT_FOUND"

  Scenario: The request does not say which offer
    When consumer 1 sends a saved offer without offerId
    Then the response has status 400
    And the field "code" is "INVALID_REQUEST"

  Scenario Outline: Only a consumer with a valid Identity token can save offers
    When offer 1 is saved <token>
    Then the response has status <status>
    And the field "code" is "<code>"
    And consumer 1 has 0 saved offers

    Examples:
      | token                                | status | code         |
      | without a token                      | 401    | UNAUTHORIZED |
      | with a token from another issuer     | 401    | UNAUTHORIZED |
      | with a token for another audience    | 401    | UNAUTHORIZED |
      | with a token signed with another key | 401    | UNAUTHORIZED |
      | with a business owner token          | 403    | FORBIDDEN    |
      | with a token without roles           | 403    | FORBIDDEN    |
