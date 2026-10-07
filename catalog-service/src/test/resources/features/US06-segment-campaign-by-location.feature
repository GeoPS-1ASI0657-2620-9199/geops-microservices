@US06 @GEO-31 @catalog
Feature: Segment campaigns by location
  As a business owner, I want to segment my campaign by zone
  so that I do not spend on an audience that will not visit me.

  Background:
    Given the business owner of business 84 "Restaurante Don Pepe" is authenticated
    And the store of business 84 is at "Av. Larco 345, Miraflores", latitude -12.1211 and longitude -77.0297

  Scenario Outline: A campaign with a zone is shown to consumers inside it
    Given the business owner published the offer "Ceviche 2x1" in a campaign with <zone>
    When a consumer searches for offers <distance> meters <direction> of the store within 20 walking minutes
    Then the search results contain "Ceviche 2x1"

    Examples:
      | zone                                                           | distance | direction |
      | a radius zone of 800 meters                                    | 500      | north     |
      | the district zone "Miraflores" centered 1500 meters south      | 500      | south     |

  Scenario Outline: A consumer outside the zone does not see the campaign
    Given the business owner published the offer "Ceviche 2x1" in a campaign with <zone>
    When a consumer searches for offers <distance> meters north of the store within 20 walking minutes
    Then the search results do not contain "Ceviche 2x1"

    Examples:
      | zone                                                           | distance |
      | a radius zone of 800 meters                                    | 1200     |
      | the district zone "Miraflores" centered 1500 meters south      | 1500     |

  Scenario: A zone without its radius is rejected
    When the business owner creates a campaign with a radius zone without radiusMeters
    Then the response has status 400
    And the field "code" is "INVALID_CAMPAIGN_ZONE"
