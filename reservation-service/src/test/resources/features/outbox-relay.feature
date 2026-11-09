@US41 @reservation @outbox
Feature: Publish the outbox to Kafka
  The redemption stores ReservationRedeemed in its own transaction; a relay publishes it to
  reservation.events, and the redemption does not depend on Kafka being up.

  Background:
    Given business 84 has an ACTIVE reservation with code "7KQ2M9XA" for offer 1052 expiring at "2026-10-21T04:59:59Z"
    And the current time is "2026-10-20T17:42:10Z"

  Scenario: The redemption reaches the reservation.events topic
    When business owner of business 84 redeems code "7KQ2M9XA"
    Then the response has status 200
    And the outbox row of the event is marked as published
    And the ReservationRedeemed event of the reservation arrives at topic "reservation.events" with its eventId

  Scenario: The redemption answers while Kafka is stopped and the event leaves when Kafka is back
    Given Kafka is stopped
    When business owner of business 84 redeems code "7KQ2M9XA"
    Then the response has status 200
    And after 8 seconds the outbox row of the event is still unpublished
    When Kafka is back
    Then the outbox row of the event is marked as published
    And the ReservationRedeemed event of the reservation arrives at topic "reservation.events" with its eventId
