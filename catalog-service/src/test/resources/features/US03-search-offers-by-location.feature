@US03 @GEO-29 @catalog
Feature: Search offers by location
  As a consumer, I want to see the offers ordered by how close they are to my location
  so that I can choose the ones I can use without going far.

  Background:
    Given the search origin is latitude -12.1211 and longitude -77.0297

  Scenario: Valid offers come ordered by distance with the distance shown
    Given these valid offers exist around the origin, each from its own business:
      | title     | meters north |
      | Juguería  | 700          |
      | Panadería | 150          |
      | Barbería  | 420          |
    When I search nearby offers with radiusMinutes 10
    Then the offers come in this order:
      | title     | distanceMeters | walkMinutes |
      | Panadería | 150            | 2           |
      | Barbería  | 420            | 6           |
      | Juguería  | 700            | 9           |

  Scenario: Inside the same 100 meter band the business without open reports goes first
    Given these valid offers exist around the origin, each from its own business:
      | title     | meters north | open reports |
      | Pizzería  | 210          | 2            |
      | Heladería | 260          | 0            |
    When I search nearby offers with radiusMinutes 5
    Then the offers come in this order:
      | title     | distanceMeters | walkMinutes |
      | Heladería | 260            | 4           |
      | Pizzería  | 210            | 3           |

  Scenario: Without location permission the search starts from the center of the chosen district
    Given these valid offers exist around the origin, each from its own business:
      | title          | meters north |
      | Menú ejecutivo | 300          |
    And the consumer denies the location permission and picks a district centered at the origin
    When I search nearby offers from the district center with radiusMinutes 10
    Then the response contains only the offer "Menú ejecutivo"

  Scenario: A zone without valid offers answers an empty list
    Given there are no valid offers within 1600 meters of the origin
    When I search nearby offers with radiusMinutes 20
    Then the response has status 200
    And the list is empty with totalElements 0
