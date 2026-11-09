@US14 @catalog
Feature: Cache the catalog by geographic cell
  As the technical owner, I want the proximity search to be served from a cache by geographic cell
  so that the read demand can grow without multiplying the load on the database.

  Background:
    Given the search origin is latitude -12.1211 and longitude -77.0297
    And a valid offer "Menú ejecutivo" exists 200 meters north of the origin

  Scenario: A repeated search on the same cell is served without the database
    Given I searched within 10 minutes once
    When I search within 10 minutes again
    Then the result has "Menú ejecutivo"
    And the database was not queried for the cells of the second search

  Scenario: A published campaign shows up in the next search of its cell
    Given I searched within 10 minutes once
    When business 84 publishes a campaign with offer "Ceviche 2x1" 300 meters north of the origin
    And I search within 10 minutes again
    Then the result has "Ceviche 2x1"

  Scenario: An expired offer disappears from the next search of its cell
    Given I searched within 10 minutes once
    And the date moves to the day after "Menú ejecutivo" expires
    When I search within 10 minutes again
    Then the result does not have "Menú ejecutivo"

  Scenario: The search still works when the cache is down
    Given the cache is unavailable
    When I search within 10 minutes
    Then the response has status 200
    And the result has "Menú ejecutivo"
