@GEO-207 @catalog
Feature: Tell Reservation whether an offer can be reserved
  As Reservation Service, I want to know whether an offer is still valid, with its title and
  business, so that I only create reservations for offers that can be redeemed.

  Background:
    Given business 84 "Restaurante Don Pepe" has an active campaign

  Scenario: A published offer inside its validity is available
    Given the campaign of business 84 has the offer "2x1 en almuerzos ejecutivos" valid until "2026-10-15"
    When Reservation asks for the availability of that offer
    Then the response has status 200
    And the response only has the fields offerId, businessId, title, validTo and available
    And the field "businessId" is "84"
    And the field "title" is "2x1 en almuerzos ejecutivos"
    And the field "available" is "true"

  Scenario: An offer past its validity is not available
    Given the campaign of business 84 has the offer "Desayuno 2x1" valid until "2026-10-07"
    When Reservation asks for the availability of that offer
    Then the response has status 200
    And the field "available" is "false"

  Scenario: An unknown offer is reported as not found
    When Reservation asks for the availability of offer 9999
    Then the response has status 404
    And the field "code" is "OFFER_NOT_FOUND"
