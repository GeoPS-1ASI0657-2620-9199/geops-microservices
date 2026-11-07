@US13 @engagement
Feature: Follow a business
  As a consumer I follow businesses to hear about their new offers.

  Background:
    Given business 84 is called "Restaurante Don Pepe"
    And offer 1052 of business 84 titled "2x1" is published until "2026-11-15"
    And offer 1040 of business 84 titled "Old offer" is published until "2026-10-01"

  Scenario: A new follow enables notices
    When consumer 2001 follows business 84
    Then the response has status 201
    And there is 1 BusinessFollowed event for consumer 2001

  Scenario: Unfollowing removes the business
    When consumer 2001 follows business 84
    And consumer 2001 unfollows business 84
    Then the response has status 204
    And consumer 2001 follows no business
    And there is 1 BusinessUnfollowed event for consumer 2001

  Scenario: Only published and valid offers can be opened from the list
    When consumer 2001 follows business 84
    And consumer 2001 lists followed businesses
    Then the response has status 200
    And the followed business has 1 active offers

  Scenario: Following twice keeps one follow and one event
    When consumer 2001 follows business 84
    And consumer 2001 follows business 84
    Then the response has status 200
    And there is 1 BusinessFollowed event for consumer 2001
