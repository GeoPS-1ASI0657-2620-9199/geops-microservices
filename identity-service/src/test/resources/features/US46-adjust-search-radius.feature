@US46 @identity
Feature: Adjust the search radius
  As a consumer, I want to adjust the walking radius of the search
  so that I only see offers I am willing to walk to.

  Background:
    Given a consumer with a session

  Scenario: The consumer saves the radius and reads it back
    When the consumer saves a radius of 15 minutes
    Then the response has status 200
    And the field "searchRadiusMinutes" is "15"
    And one ConsumerPreferencesChanged event with radius 15 is stored in the outbox
    When the consumer reads the profile
    Then the field "searchRadiusMinutes" is "15"

  Scenario: A radius outside 5 to 20 minutes is rejected
    When the consumer saves a radius of 25 minutes
    Then the response has status 400
    And the field "code" is "RADIUS_OUT_OF_RANGE"
    And the consumer keeps a radius of 10 minutes

  Scenario: A business token cannot save a consumer radius
    When a business owner saves a radius of 15 minutes
    Then the response has status 403
