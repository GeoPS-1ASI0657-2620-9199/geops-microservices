@US02 @GEO-28 @catalog
Feature: Query offers by distance radius
  As the technical lead, I want the catalog to publish a query of offers by radius
  so that the list and the map of the web app are served by a single operation.

  Background:
    Given the search origin is latitude -12.1211 and longitude -77.0297

  Scenario: Only valid offers inside the radius are returned
    Given these offers exist around the origin:
      | title            | meters north | validity | campaign |
      | Ceviche 2x1      | 300          | valid    | active   |
      | Pollo a la brasa | 900          | valid    | active   |
      | Menú vencido     | 300          | expired  | active   |
      | Café en pausa    | 300          | valid    | paused   |
    When I search nearby offers with radiusMinutes 10
    Then the response has status 200
    And the response contains only the offer "Ceviche 2x1"
    And the offer "Ceviche 2x1" is 300 meters and 4 minutes on foot away
    And the field "totalElements" is "1"

  Scenario: The query answers under two seconds at the 95th percentile
    Given the catalog has the 10000 measurement offers spread over Lima
    When I search nearby offers with radiusMinutes 20 one hundred times in a row
    Then the 95th percentile of the response time is under 2000 milliseconds

  Scenario Outline: A radius outside 5 to 20 minutes is rejected with the valid range
    When I search nearby offers with radiusMinutes <minutes>
    Then the response has status 400
    And the field "code" is "RADIUS_OUT_OF_RANGE"
    And the field "message" is "El radio debe estar entre 5 y 20 minutos a pie"

    Examples:
      | minutes |
      | 4       |
      | 21      |

  Scenario: Coordinates out of range are rejected
    When I search nearby offers from latitude 95 and longitude -77.0297 with radiusMinutes 10
    Then the response has status 400
    And the field "code" is "INVALID_COORDINATES"

  Scenario: A missing radius is named in the answer
    When I search nearby offers without radiusMinutes
    Then the response has status 400
    And the field "code" is "MISSING_PARAMETER"
    And the field "message" is "Falta el parámetro radiusMinutes"

  Scenario: A page larger than 20 offers is rejected
    When I search nearby offers with radiusMinutes 10 and page size 50
    Then the response has status 400
    And the field "code" is "INVALID_PAGE"
