@US44 @catalog
Feature: Rank businesses by their reports
  As the technical owner, I want the position of a business in the results to reflect its compliance
  history so that the guarantee of the offer is verifiable and not a promise.

  Background:
    Given the search origin is latitude -12.1211 and longitude -77.0297
    And business 84 "Restaurante Don Pepe" has offer "Menú ejecutivo" 220 meters north of the origin
    And business 85 "Cevichería La Punta" has offer "Ceviche 2x1" 250 meters north of the origin
    And businesses 84 and 85 have a verified RUC

  Scenario: A business with an open report goes below the ones without reports at the same distance
    Given I searched within 10 minutes once
    And the titles are "Menú ejecutivo, Ceviche 2x1" in that order
    When Reservation publishes ReportOpened for business 84
    And I search within 10 minutes again
    Then the titles are "Ceviche 2x1, Menú ejecutivo" in that order

  Scenario: A compliance index below 90 % removes the seal and records the reason
    Given Reservation published 8 ReservationRedeemed events for business 84
    And I searched within 10 minutes once
    When Reservation publishes ReportOpened and then ReportResolved in favor of the consumer for business 84
    Then business 84 has compliance index 88.89 and no verified seal
    And business 84 has a seal change with reason "COMPLIANCE_BELOW_THRESHOLD"
    When I search within 10 minutes again
    Then "Menú ejecutivo" has verifiedSeal false

  Scenario: Recovering the threshold restores the seal
    Given business 84 lost the seal with 8 redemptions and 1 report in favor of the consumer
    When Reservation publishes 1 ReservationRedeemed event for business 84
    Then business 84 has compliance index 90.00 and a verified seal
    And business 84 has a seal change with reason "COMPLIANCE_RESTORED"
    When I search within 10 minutes
    Then "Menú ejecutivo" has verifiedSeal true

  Scenario: An answered report no longer counts as open
    Given Reservation published ReportOpened for business 84
    When Reservation publishes ReportAnswered for business 84
    Then business 84 has 0 open reports
    When I search within 10 minutes
    Then the titles are "Menú ejecutivo, Ceviche 2x1" in that order

  Scenario: The same event twice is applied once
    When Reservation publishes the same ReportOpened event for business 84 twice
    Then business 84 has 1 open report
