@US40 @GEO-208 @reservation
Feature: Reserve an offer on the platform without paying
  As a consumer, I want to reserve an offer and pay at the store so that I do not risk money up front.

  Background:
    Given Catalog has offer 1052 from business 301 titled "Menú ejecutivo a mitad de precio" valid until "2026-10-14"

  Scenario: The offer is valid and the reservation asks for no payment method
    When consumer 2001 reserves offer 1052
    Then the response has status 201
    And the response only has the fields reservationId, code and expiresAt
    And the field "code" has 8 characters
    And the field "expiresAt" is "2026-10-15T04:59:59Z"
    And the Location header points to the created reservation
    And the stored reservation is ACTIVE with the title "Menú ejecutivo a mitad de precio" and business 301

  Scenario: The offer has already expired
    Given Catalog has offer 1053 from business 301 titled "Desayuno 2x1" valid until "2026-01-01"
    When consumer 2001 reserves offer 1053
    Then the response has status 409
    And the field "code" is "OFFER_NOT_AVAILABLE"
    And no reservation was stored

  Scenario: Catalog reports that the offer is no longer available
    Given Catalog marks offer 1052 as unavailable
    When consumer 2001 reserves offer 1052
    Then the response has status 409
    And the field "code" is "OFFER_NOT_AVAILABLE"
    And no reservation was stored

  Scenario: The offer does not exist
    When consumer 2001 reserves offer 9999
    Then the response has status 404
    And the field "code" is "OFFER_NOT_FOUND"

  Scenario: Catalog does not respond
    Given Catalog does not respond
    When consumer 2001 reserves offer 1052
    Then the response has status 503
    And the field "code" is "CATALOG_UNAVAILABLE"
    And no reservation was stored

  Scenario: Repeating the reservation of an offer with an active reservation returns the same one
    Given consumer 2001 already reserved offer 1052
    When consumer 2001 reserves offer 1052
    Then the response has status 200
    And the response is the same reservation as before
    And consumer 2001 has 1 ACTIVE reservation for offer 1052

  Scenario: Simultaneous requests for the same reservation create only one
    When consumer 2001 sends 5 simultaneous reservations for offer 1052
    Then one response has status 201 and the others 200 with the same reservation
    And consumer 2001 has 1 ACTIVE reservation for offer 1052

  Scenario: A redeemed reservation does not prevent reserving the same offer again
    Given consumer 2001 already reserved offer 1052
    And that reservation was already redeemed
    When consumer 2001 reserves offer 1052
    Then the response has status 201
    And the response is a new reservation
    And consumer 2001 has 1 ACTIVE reservation for offer 1052

  Scenario: The request does not specify the offer
    When consumer 2001 sends a reservation without offerId
    Then the response has status 400
    And the field "code" is "INVALID_REQUEST"

  Scenario Outline: Only a consumer with a valid Identity token can reserve
    When offer 1052 is reserved <token>
    Then the response has status <status>
    And the field "code" is "<code>"
    And no reservation was stored

    Examples:
      | token                                | status | code         |
      | without a token                      | 401    | UNAUTHORIZED |
      | with a token from another issuer     | 401    | UNAUTHORIZED |
      | with a token for another audience    | 401    | UNAUTHORIZED |
      | with a token signed with another key | 401    | UNAUTHORIZED |
      | with a business owner token          | 403    | FORBIDDEN    |
      | with a token without roles           | 403    | FORBIDDEN    |
