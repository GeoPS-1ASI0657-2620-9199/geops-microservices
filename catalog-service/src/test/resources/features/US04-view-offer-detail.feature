@US04 @GEO-23 @catalog
Feature: View the detail of an offer
  As a consumer, I want to see the detail of an offer with its validity and conditions
  so that I can decide whether it suits me.

  Background:
    Given business 84 "Restaurante Don Pepe" has an active campaign

  Scenario: The detail shows price, validity, conditions and location
    Given the campaign of business 84 has this offer:
      | title       | 2x1 en almuerzos ejecutivos                                     |
      | price       | 15.00                                                           |
      | validTo     | 2026-10-15                                                      |
      | conditions  | Válido de lunes a viernes de 12:00 a 15:00. Un cupón por mesa. |
      | address     | Av. Larco 345, Miraflores                                       |
      | latitude    | -12.1211                                                        |
      | longitude   | -77.0297                                                        |
    When a consumer opens the detail of that offer
    Then the response has status 200
    And the detail shows the price, validity, conditions, address and coordinates of that offer
    And the field "businessName" is "Restaurante Don Pepe"
    And the field "available" is "true"

  Scenario Outline: An expired offer is shown as not available
    Given the campaign of business 84 has an offer valid until "<validTo>" with status <status>
    When a consumer opens the detail of that offer
    Then the response has status 200
    And the field "available" is "false"

    Examples:
      | validTo    | status    |
      | 2026-10-07 | PUBLISHED |
      | 2026-10-07 | EXPIRED   |

  Scenario: An unknown offer is reported as not found
    When a consumer opens the detail of offer 9999
    Then the response has status 404
    And the field "code" is "OFFER_NOT_FOUND"
