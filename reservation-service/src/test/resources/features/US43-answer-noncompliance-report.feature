@US43 @reservation
Feature: Answer a noncompliance report
  As a business owner, I want to answer a report to explain what happened
  before it affects my position in the results.

  Background:
    Given the current time is "2026-10-29T12:00:00Z"
    And consumer 2001 reported reservation 5013 of business 84 at "2026-10-28T19:10:00Z" with reason "No me aplicaron el 2x1."

  Scenario: The business sees the reason, the reservation and the time left
    When the business owner of business 84 lists the reports
    Then the list has 1 report with status "OPEN"
    And the report has reason "No me aplicaron el 2x1.", reservation code "Q4ZT8K2M" and responseDeadline "2026-10-30T19:10:00Z"

  Scenario: An answer within the deadline moves the case to review and informs the consumer
    When the business owner of business 84 answers the report with "El 2x1 aplicaba de lunes a jueves."
    Then the response has status 201
    And the response shows the answer "El 2x1 aplicaba de lunes a jueves."
    And the report is stored as "IN_REVIEW"
    And a ReportAnswered event for reservation 5013 is in the outbox

  Scenario: The consumer sees the answer of the business
    Given the business owner of business 84 answered the report with "El 2x1 aplicaba de lunes a jueves."
    When consumer 2001 opens the report
    Then the response has status 200
    And the response shows the answer "El 2x1 aplicaba de lunes a jueves."

  Scenario: Resolving in favor of the consumer closes the report
    When the business owner of business 84 resolves the report in favor of the consumer
    Then the response has status 200
    And the response has status "CLOSED" and resolution "CONSUMER_FAVOR"
    And a ReportResolved event with closedBy "BUSINESS" and previousStatus "OPEN" is in the outbox

  Scenario: Resolving after an answer keeps the previous status in the event
    Given the business owner of business 84 answered the report with "El 2x1 aplicaba de lunes a jueves."
    When the business owner of business 84 resolves the report in favor of the consumer
    Then the response has status 200
    And a ReportResolved event with closedBy "BUSINESS" and previousStatus "IN_REVIEW" is in the outbox

  Scenario: A closed report cannot be resolved again
    Given the business owner of business 84 answered the report with "El 2x1 aplicaba de lunes a jueves."
    When the business owner of business 84 resolves the report in favor of the consumer
    And the business owner of business 84 resolves the report in favor of the consumer
    Then the response has status 409
    And the field "code" is "REPORT_ALREADY_CLOSED"

  Scenario: The business cannot resolve in its own favor
    When the business owner of business 84 resolves the report in favor of the business
    Then the response has status 403
    And the report is stored as "OPEN"

  Scenario: An answer after the deadline is rejected
    Given the current time is "2026-10-30T19:11:00Z"
    When the business owner of business 84 answers the report with "Llegamos tarde a responder."
    Then the response has status 410
    And the field "code" is "REPORT_RESPONSE_DEADLINE_PASSED"
    And no report event is in the outbox

  Scenario: A second answer is rejected
    Given the business owner of business 84 answered the report with "El 2x1 aplicaba de lunes a jueves."
    When the business owner of business 84 answers the report with "Otra respuesta distinta."
    Then the response has status 409
    And the field "code" is "REPORT_NOT_OPEN"

  Scenario: Another business cannot see or answer the report
    When the business owner of business 99 lists the reports
    Then the list has 0 report with status "OPEN"
    When the business owner of business 99 answers the report with "No es mío, pero respondo."
    Then the response has status 403
    And the report is stored as "OPEN"

  Scenario: The consumer sees only their own reports
    When consumer 2001 lists their reports
    Then the list has 1 report with status "OPEN"
    When consumer 2002 lists their reports
    Then the list has 0 report with status "OPEN"
