@US47 @notification
Feature: Receive nearby offer notices by email
  As a consumer I receive nearby offers in one email per configured window.

  Background:
    Given consumer 2001 has a confirmed email with email on and a limit of 3

  Scenario: A nearby offer includes the offer distance and validity
    When Catalog publishes 1 nearby offers in the same window
    And the email window closes
    Then the consumer receives 1 grouped email with 1 offers
    And the grouped email includes the offer distance validity and unsubscribe link

  Scenario: A signed unsubscribe link prevents further nearby offer email
    Given consumer 2001 used the nearby email unsubscribe link
    When Catalog publishes 1 nearby offers in the same window
    And the email window closes
    Then the consumer receives 0 grouped email with 0 offers

  Scenario: Several offers in one window arrive as one email
    When Catalog publishes 3 nearby offers in the same window
    And the email window closes
    Then the consumer receives 1 grouped email with 3 offers
