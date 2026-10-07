@US01 @GEO-26 @catalog
Feature: Model offer coordinates and spatial index
  As the technical lead, I want the location of each offer stored as coordinates with a spatial index
  so that offers can be searched by proximity.

  Scenario: The offer location is stored as a pair of coordinates
    Given the offer "Menú del día" is saved at latitude -12.1211 and longitude -77.0297
    When the offer is read from the catalog
    Then its location has latitude -12.1211 and longitude -77.0297

  Scenario: The search by area uses the spatial index
    Given the catalog has the 10000 measurement offers spread over Lima
    When the execution plan of a search within 1600 meters of -12.1211, -77.0297 is read
    Then the plan uses the index "ix_offers_location"

  Scenario Outline: A location that is not a valid pair of coordinates is rejected
    When an offer is registered at latitude <latitude> and longitude <longitude>
    Then the registration is rejected with the reason "La latitud debe estar entre -90 y 90 y la longitud entre -180 y 180"

    Examples:
      | latitude | longitude |
      | 95       | -77.0297  |
      | -12.1211 | -200      |
