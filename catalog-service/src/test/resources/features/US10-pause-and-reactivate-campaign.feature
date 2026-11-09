@US10 @catalog
Feature: Pause and reactivate a campaign
  As a business owner, I want to pause and reactivate a campaign so that I control when my offer is shown.

  Background:
    Given the search origin is latitude -12.1211 and longitude -77.0297
    And the current date in Lima is "2026-10-28"
    And business 84 has an active campaign "Almuerzos de noviembre" until "2026-11-15" with offer "2x1 en almuerzos ejecutivos" 200 meters north of the origin
    And I searched within 10 minutes once

  Scenario: A paused campaign is no longer shown in the catalog
    When business owner of business 84 sets campaign "Almuerzos de noviembre" to "PAUSED"
    Then the response has status 200
    And the field "status" is "PAUSED"
    And a CampaignPaused event of campaign "Almuerzos de noviembre" with offer "2x1 en almuerzos ejecutivos" is in the outbox
    When I search within 10 minutes again
    Then the result does not have "2x1 en almuerzos ejecutivos"

  Scenario: A reactivated campaign is shown again while its validity has not ended
    Given business owner of business 84 set campaign "Almuerzos de noviembre" to "PAUSED"
    And I search within 10 minutes again
    When business owner of business 84 sets campaign "Almuerzos de noviembre" to "ACTIVE"
    Then the response has status 200
    And the field "status" is "ACTIVE"
    And an OfferPublished event of campaign "Almuerzos de noviembre" marked as reactivated is in the outbox
    When I search within 10 minutes again
    Then the result has "2x1 en almuerzos ejecutivos"

  Scenario: Pausing a campaign that is already paused changes nothing
    Given business owner of business 84 set campaign "Almuerzos de noviembre" to "PAUSED"
    When business owner of business 84 sets campaign "Almuerzos de noviembre" to "PAUSED"
    Then the response has status 200
    And the field "status" is "PAUSED"
    And a CampaignPaused event of campaign "Almuerzos de noviembre" with offer "2x1 en almuerzos ejecutivos" is in the outbox

  Scenario: A campaign whose validity ended cannot be reactivated
    Given business owner of business 84 set campaign "Almuerzos de noviembre" to "PAUSED"
    And the current date in Lima is "2026-11-16"
    When business owner of business 84 sets campaign "Almuerzos de noviembre" to "ACTIVE"
    Then the response has status 409
    And the field "code" is "CAMPAIGN_ALREADY_ENDED"

  Scenario: An offer of a paused campaign cannot be reserved
    Given business owner of business 84 set campaign "Almuerzos de noviembre" to "PAUSED"
    When Reservation asks for the availability of offer "2x1 en almuerzos ejecutivos"
    Then the response has status 200
    And the field "available" is "false"

  Scenario: An offer of a reactivated campaign can be reserved again
    Given business owner of business 84 set campaign "Almuerzos de noviembre" to "PAUSED"
    And business owner of business 84 set campaign "Almuerzos de noviembre" to "ACTIVE"
    When Reservation asks for the availability of offer "2x1 en almuerzos ejecutivos"
    Then the field "available" is "true"

  Scenario: Only PAUSED and ACTIVE can be asked for
    When business owner of business 84 sets campaign "Almuerzos de noviembre" to "FINISHED"
    Then the response has status 400
    And the field "code" is "INVALID_CAMPAIGN_STATUS"

  Scenario: Another business cannot pause the campaign
    When business owner of business 99 sets campaign "Almuerzos de noviembre" to "PAUSED"
    Then the response has status 403
    And no CampaignPaused event of campaign "Almuerzos de noviembre" is in the outbox
