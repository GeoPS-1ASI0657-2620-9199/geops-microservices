@US07 @catalog
Feature: Geocode the locations stored as text
  As the technical owner, I want offers with a text location to get coordinates
  so that they take part in the proximity search.

  Background:
    Given the administrator loaded these public source offers:
      | title                  | address                              |
      | Feria de libros usados | Jr. de la Unión 300, Cercado de Lima |
      | Clases de marinera     | Calle que no existe 999              |
    And Nominatim finds "Jr. de la Unión 300, Cercado de Lima" at latitude -12.0464 and longitude -77.0306
    And Nominatim finds nothing for "Calle que no existe 999"

  Scenario: Each offer ends with valid coordinates or marked for manual review
    When the administrator runs the geocoding
    Then "Feria de libros usados" is GEOCODED at latitude -12.0464 and longitude -77.0306
    And "Clases de marinera" is in MANUAL_REVIEW

  Scenario: The report tells how many were converted and how many need review
    When the administrator runs the geocoding
    Then the run reports 1 geocoded and 1 in manual review
    And the geocoding summary shows 1 geocoded and 1 in manual review

  Scenario: A second run does not change the converted locations
    Given the administrator ran the geocoding
    And Nominatim now finds "Jr. de la Unión 300, Cercado de Lima" at latitude -12.0500 and longitude -77.0400
    When the administrator runs the geocoding again
    Then the run reports 0 processed
    And "Feria de libros usados" is still at latitude -12.0464 and longitude -77.0306

  Scenario: Nominatim is down and the offers wait for the next run
    Given Nominatim answers 503
    When the administrator runs the geocoding
    Then both offers are still PENDING
    And the run reports that it stopped because of the provider

  Scenario: Only the administrator runs the geocoding
    When a business owner runs the geocoding
    Then the response has status 403
