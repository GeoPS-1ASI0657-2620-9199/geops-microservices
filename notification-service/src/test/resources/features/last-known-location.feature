@US29 @GEO-210 @notification
Feature: Record the last known location
  As a consumer, I want the platform to keep my last known location so that nearby notices only use a recent one.

  Background:
    Given consumer 1 has the recipient copy "lucia.fernandez@ejemplo.pe" with a confirmed email

  Scenario: A location captured now is stored and is fresh
    When consumer 1 sends latitude "-12.1211", longitude "-77.0297", accuracy 25 captured at "2026-10-08T13:04:30Z"
    Then the response has status 200
    And the field "fresh" is "true"
    And the field "radiusMeters" is "800"
    And the field "capturedAt" is "2026-10-08T13:04:30Z"

  Scenario: A location captured 61 minutes ago is stored but is not fresh
    When consumer 1 sends latitude "-12.1211", longitude "-77.0297", accuracy 25 captured at "2026-10-08T12:04:00Z"
    Then the response has status 200
    And the field "fresh" is "false"

  Scenario: A capture time in the future is rejected
    When consumer 1 sends latitude "-12.1211", longitude "-77.0297", accuracy 25 captured at "2026-10-08T13:08:00Z"
    Then the response has status 400
    And the field "code" is "INVALID_CAPTURE_TIME"
    And consumer 1 has no stored location

  Scenario: An older reading does not replace a newer one
    Given consumer 1 sent a location captured at "2026-10-08T13:00:00Z"
    When consumer 1 sends latitude "-12.0500", longitude "-77.0400", accuracy 30 captured at "2026-10-08T12:50:00Z"
    Then the response has status 200
    And the field "capturedAt" is "2026-10-08T13:00:00Z"
    And the field "latitude" is "-12.1211"

  Scenario: The first location creates the preferences with both channels off
    When consumer 1 sends latitude "-12.1211", longitude "-77.0297", accuracy 25 captured at "2026-10-08T13:04:30Z"
    Then the response has status 200
    And the stored preference of consumer 1 has push false, email false and daily limit 3

  Scenario: A latitude out of range is rejected
    When consumer 1 sends latitude "95", longitude "-77.0297", accuracy 25 captured at "2026-10-08T13:04:30Z"
    Then the response has status 400
    And the field "code" is "INVALID_REQUEST"
    And consumer 1 has no stored location

  Scenario: The stored position is a geography point with SRID 4326
    When consumer 1 sends latitude "-12.1211", longitude "-77.0297", accuracy 25 captured at "2026-10-08T13:04:30Z"
    Then the stored position of consumer 1 is a point with SRID 4326 at latitude -12.1211 and longitude -77.0297

  Scenario: A consumer whose recipient copy has not arrived cannot record a location
    When consumer 2 sends latitude "-12.1211", longitude "-77.0297", accuracy 25 captured at "2026-10-08T13:04:30Z"
    Then the response has status 404
    And the field "code" is "RECIPIENT_NOT_FOUND"
    And consumer 2 has no stored location
