@US34 @GEO-189 @reservation
Feature: Isolate a Catalog outage with a circuit breaker
  As the technical lead, I want the call from Reservation to Catalog behind a circuit breaker with a timeout and a
  limit of calls in flight so that a Catalog outage does not hold the reservation threads.

  Background:
    Given Catalog is reached through its real client and answers with errors

  Scenario: Five failures in ten seconds open the circuit and the next reservation is rejected at once
    When consumer 2001 tries to reserve offer 1052 5 times
    Then every attempt has status 503 with the code "CATALOG_UNAVAILABLE"
    And the Catalog circuit is OPEN
    When consumer 2001 reserves offer 1052 once more
    Then the response has status 503
    And the field "code" is "CATALOG_UNAVAILABLE"
    And the message tells the consumer in Spanish to try again later
    And the answer took less than 500 milliseconds
    And Catalog received 5 requests
    And no reservation was stored

  Scenario: An offer that does not exist is an answer of Catalog, not a failure
    Given Catalog answers that offer 9999 does not exist
    When consumer 2001 tries to reserve offer 9999 6 times
    Then every attempt has status 404 with the code "OFFER_NOT_FOUND"
    And the Catalog circuit is CLOSED

  Scenario: Once the open wait has passed, a successful probe closes the circuit
    Given consumer 2001 tries to reserve offer 1052 5 times
    And the Catalog circuit is OPEN
    And Catalog answers again that offer 1052 is available
    And the open wait of the Catalog circuit has passed
    When consumer 2001 reserves offer 1052 once more
    Then the response has status 201
    And the Catalog circuit is CLOSED
