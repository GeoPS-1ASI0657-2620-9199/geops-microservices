@US34 @GEO-189 @notification
Feature: Keep notifying when a delivery provider fails
  As the technical lead, I want FCM and the mail server behind circuit breakers with a timeout and a limit of calls
  in flight so that the outage of one provider does not hold the event consumers nor lose the notices.

  Scenario: With FCM down the circuit opens and the notices still arrive by email
    Given consumer 2001 allowed push and email with a confirmed address and a limit of 10
    And FCM does not answer
    When Catalog publishes 6 nearby offers in the same window
    Then FCM was called 5 times
    And the FCM circuit is OPEN
    And the consumer receives 0 recorded web push
    When the email window closes
    Then the consumer receives 1 grouped email with 6 offers

  Scenario: With the mail server down the digest stays pending and is sent once the circuit closes
    Given consumer 2001 has a confirmed email with email on and a limit of 10
    And the mail server does not answer
    When Catalog publishes 1 nearby offers in the same window
    And the email window closes 6 times
    Then the mail server was called 5 times
    And the SMTP circuit is OPEN
    And the consumer receives 0 grouped email with 0 offers
    Given the mail server answers again
    And the open wait of the SMTP circuit has passed
    When the email window closes
    Then the consumer receives 1 grouped email with 1 offers
    And the SMTP circuit is CLOSED
