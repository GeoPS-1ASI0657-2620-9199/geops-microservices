@US33 @platform
Feature: Communicate the services asynchronously
  As the technical owner, I want the events between services to be published asynchronously
  so that a service that is down does not stop the main operation.

  Background:
    Given the platform is running with Kafka
    And a business owner has an ACTIVE reservation code of a consumer

  Scenario: The redemption completes while the consumer service is down
    Given engagement-service is stopped
    When the business owner redeems the code through the gateway
    Then the response has status 200
    And the ReservationRedeemed event is in topic "reservation.events"
    And the consumer group "engagement-service" has a lag of at least 1

  Scenario: The consumer processes the pending events when it comes back
    Given engagement-service is stopped
    And the business owner redeemed the code through the gateway
    When engagement-service starts again
    Then within 60 seconds the reservation is in redeemed_reservations of engagement_db once
    And the consumer group "engagement-service" has a lag of 0

  Scenario: An event that cannot be processed is kept for review
    When an unreadable record is published to "reservation.events"
    Then within 10 seconds it is in "reservation.events.DLT"
    And the logs of engagement-service show "event.dead-lettered" with its offset
    And the next valid event is still processed
