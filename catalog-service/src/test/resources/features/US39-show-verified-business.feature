@US39 @catalog
Feature: Show which businesses are verified in the results
  As a consumer, I want to recognize verified businesses so that I can trust the offer before I walk there.

  Background:
    Given the search origin is latitude -12.1211 and longitude -77.0297
    And business 84 "Restaurante Don Pepe" has offer "Menú ejecutivo" 200 meters north of the origin

  Scenario: A business with a valid seal shows it next to its name
    Given Identity published BusinessVerified for business 84 with verified true
    When I search within 10 minutes
    Then "Menú ejecutivo" has verifiedSeal true and business name "Restaurante Don Pepe"

  Scenario: A public source offer is listed apart and without guarantee
    Given a GEOCODED public source offer "Feria de libros usados" from "Municipalidad de Lima" 300 meters north of the origin
    When I search within 10 minutes
    Then the main result does not have "Feria de libros usados"
    And the public sources result has "Feria de libros usados" from "Municipalidad de Lima"

  Scenario: A business that loses the seal is no longer shown as verified
    Given Identity published BusinessVerified for business 84 with verified true
    And I searched within 10 minutes once
    When Identity publishes BusinessVerified for business 84 with verified false
    And I search within 10 minutes again
    Then "Menú ejecutivo" has verifiedSeal false

  Scenario: The same event twice changes the seal once
    When Identity publishes the same BusinessVerified event for business 84 twice
    Then business 84 has one seal change recorded

  Scenario: An event Catalog cannot read does not stop the consumer
    When Identity publishes an event Catalog cannot read and then BusinessVerified for business 84
    Then business 84 has the verified seal

  Scenario: An event that fails because the database is down is applied when it comes back
    When Identity publishes BusinessVerified for business 84 while the database is down for 8 seconds
    Then business 84 has the verified seal
    And business 84 has one seal change recorded
