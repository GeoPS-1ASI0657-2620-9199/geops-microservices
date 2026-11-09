@US46 @catalog
Feature: Adjust the search radius
  As a consumer, I want to set how far I am willing to walk so that I only see the offers I can reach.

  Background:
    Given the search origin is latitude -12.1211 and longitude -77.0297
    And these valid offers exist around the origin, each from its own business:
      | title          | meters north |
      | Panadería      | 350          |
      | Menú ejecutivo | 1500         |

  Scenario: Five minutes on foot limits the results to 400 meters
    When I search within 5 minutes
    Then the titles are "Panadería" in that order

  Scenario: Twenty minutes on foot reaches 1600 meters
    When I search within 20 minutes
    Then the titles are "Panadería, Menú ejecutivo" in that order

  Scenario: A radius outside five to twenty minutes is rejected
    When I search within 25 minutes with category "Gastronomía"
    Then the response has status 400
    And the field "code" is "RADIUS_OUT_OF_RANGE"
