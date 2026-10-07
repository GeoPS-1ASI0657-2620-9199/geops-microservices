@US48 @GEO-210 @notification
Feature: Configure notification preferences
  As a consumer, I want to choose the channel and how often I get notices so that the platform is not intrusive.

  Background:
    Given consumer 1 has the recipient copy "lucia.fernandez@ejemplo.pe" with a confirmed email

  Scenario: The consumer turns email on and push off with a daily limit
    When consumer 1 sets push to false, email to true and a daily limit of 3
    Then the response has status 200
    And the field "dailyLimit" is "3"
    And the field "updatedAt" is "2026-10-08T13:05:00Z"
    And the stored preference of consumer 1 has push false, email true and daily limit 3

  Scenario: Changing the preferences replaces the previous ones
    Given consumer 1 set push to false, email to true and a daily limit of 3
    When consumer 1 sets push to true, email to false and a daily limit of 5
    Then the response has status 200
    And the stored preference of consumer 1 has push true, email false and daily limit 5

  Scenario Outline: A daily limit outside 1 to 10 is rejected
    When consumer 1 sets push to true, email to true and a daily limit of <limit>
    Then the response has status 400
    And the field "code" is "INVALID_REQUEST"
    And consumer 1 has no stored preference

    Examples:
      | limit |
      | 0     |
      | 11    |

  Scenario: A channel left out of the request is rejected
    When consumer 1 sends preferences without pushEnabled
    Then the response has status 400
    And the field "code" is "INVALID_REQUEST"

  Scenario: A consumer whose recipient copy has not arrived cannot set preferences
    When consumer 2 sets push to false, email to true and a daily limit of 3
    Then the response has status 404
    And the field "code" is "RECIPIENT_NOT_FOUND"
    And consumer 2 has no stored preference

  Scenario Outline: Only a consumer with a valid Identity token can set preferences
    When the preferences are sent <token>
    Then the response has status <status>
    And the field "code" is "<code>"
    And consumer 1 has no stored preference

    Examples:
      | token                                | status | code         |
      | without a token                      | 401    | UNAUTHORIZED |
      | with a token from another issuer     | 401    | UNAUTHORIZED |
      | with a token for another audience    | 401    | UNAUTHORIZED |
      | with a token signed with another key | 401    | UNAUTHORIZED |
      | with a business owner token          | 403    | FORBIDDEN    |
      | with a token without roles           | 403    | FORBIDDEN    |
