@US09 @catalog
Feature: View offers on the map
  As a consumer, I want to see the offers on a map
  so that I can locate the businesses with respect to my position.

  Background:
    Given the search origin is latitude -12.1211 and longitude -77.0297

  Scenario: Each offer comes with the business location and its distance to place it on the map
    Given these valid offers exist around the origin, each from its own business:
      | title       | meters north |
      | Ceviche 2x1 | 300          |
      | Panadería   | 150          |
    When I search nearby offers with radiusMinutes 10
    Then the response has status 200
    And every offer in the response carries latitude, longitude and distance
    And the offer "Ceviche 2x1" is pinned at the business location 300 meters north of the origin
    And the offer "Ceviche 2x1" is 300 meters and 4 minutes on foot away
    And the offer "Panadería" is pinned at the business location 150 meters north of the origin

  Scenario: Only businesses with valid offers appear on the map
    Given these offers exist around the origin:
      | title            | meters north | validity | campaign |
      | Ceviche 2x1      | 300          | valid    | active   |
      | Menú vencido     | 300          | expired  | active   |
      | Café en pausa    | 300          | valid    | paused   |
    When I search nearby offers with radiusMinutes 10
    Then the response has status 200
    And the response contains only the offer "Ceviche 2x1"

  Scenario: A zone without valid offers shows no businesses on the map
    Given there are no valid offers within 1600 meters of the origin
    When I search nearby offers with radiusMinutes 20
    Then the response has status 200
    And the list is empty with totalElements 0
