@US42 @reservation
Feature: Report an offer that was not honored
  As a consumer, I want to report that the business did not honor the offer
  so that the platform records it and others do not go through the same.

  Background:
    Given the current time is "2026-10-28T19:10:00Z"
    And consumer 2001 has an ACTIVE reservation 5013 with code "Q4ZT8K2M" of business 84 expiring at "2026-10-29T04:59:59Z"

  Scenario: The report opens with its date and the business is told through an event
    When consumer 2001 reports reservation 5013 with reason "No me aplicaron el 2x1 y me cobraron los dos platos."
    Then the response has status 201
    And the report has status "OPEN", openedAt "2026-10-28T19:10:00Z" and responseDeadline "2026-10-30T19:10:00Z"
    And a ReportOpened event for reservation 5013 is in the outbox
    And reservation 5013 has status "REPORTED"

  Scenario: The reservation shows as reported with the response deadline
    Given consumer 2001 reported reservation 5013
    When consumer 2001 opens reservation 5013
    Then the reservation has status "REPORTED"
    And its report has responseDeadline "2026-10-30T19:10:00Z"

  Scenario: An unanswered report is resolved in favor of the consumer when the deadline passes
    Given consumer 2001 reported reservation 5013
    And the current time is "2026-10-30T19:15:00Z"
    When the deadline scheduler runs
    Then the report has status "CLOSED" and resolution "CONSUMER_FAVOR"
    And a ReportResolved event with closedBy "DEADLINE" and previousStatus "OPEN" is in the outbox

  Scenario: Running the scheduler twice closes the report once
    Given consumer 2001 reported reservation 5013
    And the current time is "2026-10-30T19:15:00Z"
    When the deadline scheduler runs
    And the deadline scheduler runs
    Then the report has status "CLOSED" and resolution "CONSUMER_FAVOR"
    And a ReportResolved event with closedBy "DEADLINE" and previousStatus "OPEN" is in the outbox

  Scenario Outline: A reservation that cannot be reported is rejected with its reason
    Given reservation 6001 is <state>
    And the current time is "2026-10-28T19:10:00Z"
    When consumer 2001 reports reservation 6001 with reason "No respetaron el precio publicado."
    Then the response has status <status>
    And the field "code" is "<code>"
    And no report event is in the outbox

    Examples:
      | state                        | status | code                       |
      | already reported             | 409    | REPORT_ALREADY_EXISTS      |
      | REDEEMED                     | 409    | RESERVATION_NOT_REPORTABLE |
      | ACTIVE but past its deadline | 410    | RESERVATION_EXPIRED        |
      | of consumer 2002             | 403    | FORBIDDEN                  |

  Scenario Outline: A reason outside ten to five hundred characters is rejected
    When consumer 2001 reports reservation 5013 with reason "<reason>"
    Then the response has status 400
    And the field "code" is "INVALID_REQUEST"
    And reservation 5013 has status "ACTIVE"

    Examples:
      | reason |
      | corto  |

  Scenario: A request without token is rejected
    When a request without token lists the reports
    Then the response has status 401

  Scenario: An active reservation past its deadline is marked expired
    Given the current time is "2026-10-29T05:10:00Z"
    When the deadline scheduler runs
    Then reservation 5013 has status "EXPIRED"
