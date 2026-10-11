@US41 @reservation
Feature: Validate the reservation code at the store
  As a business owner, I want to validate the code the consumer shows
  so that I only hand over the promotion against an authentic and valid reservation.

  Background:
    Given business 84 has an ACTIVE reservation with code "7KQ2M9XA" for offer 1052 expiring at "2026-10-21T04:59:59Z"
    And the current time is "2026-10-20T17:42:10Z"

  Scenario: The owner sees the offer, its deadline and whether it belongs to the business
    When business owner of business 84 looks up code "7KQ2M9XA"
    Then the response has status 200
    And the response shows offer 1052, its title and expiresAt "2026-10-21T04:59:59Z"

  Scenario: A code typed in lowercase and with blanks finds the same reservation
    When business owner of business 84 looks up code "7kq2 m9xa"
    Then the response has status 200
    And the response shows offer 1052, its title and expiresAt "2026-10-21T04:59:59Z"

  Scenario: A valid code within its deadline is redeemed once
    When business owner of business 84 redeems code "7KQ2M9XA"
    Then the response has status 200
    And the field "status" is "REDEEMED"
    And the field "redeemedAt" is "2026-10-20T17:42:10Z"
    And one ReservationRedeemed event for the reservation is stored in the outbox

  Scenario: A second use of the same code is rejected with the redemption date
    Given business owner of business 84 redeemed code "7KQ2M9XA"
    When business owner of business 84 redeems code "7KQ2M9XA"
    Then the response has status 409
    And the field "code" is "RESERVATION_ALREADY_REDEEMED"
    And the field "redeemedAt" is "2026-10-20T17:42:10Z"
    And there is still one ReservationRedeemed event in the outbox

  Scenario: An expired code is rejected
    Given the current time is "2026-10-21T05:00:00Z"
    When business owner of business 84 redeems code "7KQ2M9XA"
    Then the response has status 410
    And the field "code" is "RESERVATION_EXPIRED"

  Scenario: A code of another business is rejected
    When business owner of business 99 redeems code "7KQ2M9XA"
    Then the response has status 403
    And the field "message" is "This code belongs to another business"
    And the reservation is still ACTIVE

  Scenario: An unknown code is rejected
    When business owner of business 84 redeems code "ZZZZ9999"
    Then the response has status 404
    And the field "code" is "RESERVATION_NOT_FOUND"

  Scenario: A consumer token cannot redeem
    When consumer 2001 redeems code "7KQ2M9XA"
    Then the response has status 403
