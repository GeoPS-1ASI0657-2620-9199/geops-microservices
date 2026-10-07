@US05 @GEO-30 @catalog
Feature: Create an advertising campaign
  As a business owner, I want to create a campaign with its validity and its zone
  so that I reach customers near my store.

  Background:
    Given the business owner of business 84 "Restaurante Don Pepe" is authenticated
    And the store of business 84 is at "Av. Larco 345, Miraflores", latitude -12.1211 and longitude -77.0297

  Scenario: The campaign is published and visible in the catalog
    When the business owner creates the campaign "Almuerzos de octubre" from "2026-10-05" to "2026-10-31" with a radius zone of 800 meters and the offer "2x1 en almuerzos ejecutivos" valid until "2026-10-15"
    Then the response has status 201
    And the field "status" is "ACTIVE"
    And the field "businessId" is "84"
    And the offer "2x1 en almuerzos ejecutivos" is available in its public detail

  Scenario: The campaign is geo-referenced and takes part in the distance search
    When the business owner creates the campaign "Almuerzos de octubre" from "2026-10-05" to "2026-10-31" with a radius zone of 800 meters and the offer "2x1 en almuerzos ejecutivos" valid until "2026-10-15"
    And a consumer searches for offers 300 meters east of the store within 10 walking minutes
    Then the search results contain "2x1 en almuerzos ejecutivos" at about 300 meters

  Scenario: A campaign whose validity already ended cannot be published
    When the business owner creates the campaign "Septiembre" from "2026-09-01" to "2026-09-30" with a radius zone of 800 meters and the offer "Menú de septiembre" valid until "2026-09-30"
    Then the response has status 400
    And the field "code" is "CAMPAIGN_ALREADY_ENDED"
    And business 84 has no campaigns

  Scenario Outline: Only an authenticated business owner can create a campaign
    When the campaign "Almuerzos de octubre" is sent <credentials>
    Then the response has status <status>
    And the field "code" is "<code>"
    And business 84 has no campaigns

    Examples:
      | credentials                           | status | code         |
      | without a token                       | 401    | UNAUTHORIZED |
      | with a consumer token                 | 403    | FORBIDDEN    |
      | with a business token without its id  | 403    | FORBIDDEN    |
