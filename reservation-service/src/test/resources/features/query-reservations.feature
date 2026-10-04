@GEO-208 @reservation
Feature: Query reservations
  As a consumer I want to see my reservations, and as a business owner I want to see the reservation that matches a code.

  Background:
    Given Catalog has offer 1052 from business 301 titled "Menú ejecutivo a mitad de precio" valid until "2026-10-14"
    And consumer 2001 already reserved offer 1052

  Scenario: The consumer views their reservation
    When consumer 2001 views that reservation
    Then the response has status 200
    And the response shows offer 1052 titled "Menú ejecutivo a mitad de precio" with status ACTIVE
    And the field "expiresAt" is "2026-10-15T04:59:59Z"

  Scenario: The reservation does not exist
    When consumer 2001 views a reservation that does not exist
    Then the response has status 404
    And the field "code" is "RESERVATION_NOT_FOUND"

  Scenario: Another consumer cannot view the reservation
    When consumer 2002 views that reservation
    Then the response has status 403
    And the field "code" is "FORBIDDEN"

  Scenario: The business that owns the offer looks up the code
    When business 301 looks up the code of that reservation
    Then the response has status 200
    And the response shows offer 1052 titled "Menú ejecutivo a mitad de precio" with status ACTIVE
    And the field "expiresAt" is "2026-10-15T04:59:59Z"

  Scenario: Another business cannot look up the code
    When business 302 looks up the code of that reservation
    Then the response has status 403
    And the field "code" is "FORBIDDEN"

  Scenario: A consumer cannot look up by code
    When consumer 2001 looks up the code of that reservation
    Then the response has status 403
    And the field "code" is "FORBIDDEN"

  Scenario: The consumer lists their active reservations
    Given Catalog has offer 1053 from business 301 titled "Desayuno 2x1" valid until "2026-10-20"
    And consumer 2001 already reserved offer 1053
    And that reservation was already redeemed
    When consumer 2001 lists their reservations with status ACTIVE
    Then the response has status 200
    And the list only has ACTIVE reservations for offer 1052

  Scenario: The requested status does not exist
    When consumer 2001 lists their reservations with status VIGENTE
    Then the response has status 400
    And the field "code" is "INVALID_REQUEST"
