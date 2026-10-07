@GEO-207 @catalog
Feature: Query the catalog
  As a consumer I want to see an offer of the catalog, and as a business owner I want to see my campaigns and their offers.

  Background:
    Given business 1 named "Bodega Doña Rosa" has the campaign "Almuerzos de octubre" from "2026-10-05" to "2026-10-31"
    And the campaign "Almuerzos de octubre" publishes the offer "Menú ejecutivo a mitad de precio" at 12.50 until "2026-10-31"
    And business 2 named "Pollería El Sabor" has the campaign "Pollo a la brasa" from "2026-10-01" to "2026-10-20"

  Scenario: Anyone views an offer of the catalog
    When a visitor views the offer "Menú ejecutivo a mitad de precio"
    Then the response has status 200
    And the field "title" is "Menú ejecutivo a mitad de precio"
    And the field "price" is "12.5"
    And the field "validTo" is "2026-10-31"
    And the field "available" is "true"

  Scenario: The offer does not exist
    When a visitor views an offer that does not exist
    Then the response has status 404
    And the field "code" is "OFFER_NOT_FOUND"

  Scenario: The offer id is not a number
    When a visitor views the offer with id "abc"
    Then the response has status 400
    And the field "code" is "INVALID_REQUEST"

  Scenario: The business owner lists their campaigns
    When business 1 lists its campaigns
    Then the response has status 200
    And the list only has the campaign "Almuerzos de octubre"

  Scenario: The business owner views their campaign
    When business 1 views the campaign "Almuerzos de octubre"
    Then the response has status 200
    And the field "name" is "Almuerzos de octubre"
    And the field "status" is "ACTIVE"

  Scenario: The business owner cannot view a campaign of another business
    When business 1 views the campaign "Pollo a la brasa"
    Then the response has status 403
    And the field "code" is "FORBIDDEN"

  Scenario: The campaign does not exist
    When business 1 views a campaign that does not exist
    Then the response has status 404
    And the field "code" is "CAMPAIGN_NOT_FOUND"

  Scenario: The business owner lists the offers of their campaign
    When business 1 lists the offers of the campaign "Almuerzos de octubre"
    Then the response has status 200
    And the list only has the offer "Menú ejecutivo a mitad de precio"

  Scenario: The business owner cannot list the offers of a campaign of another business
    When business 1 lists the offers of the campaign "Pollo a la brasa"
    Then the response has status 403
    And the field "code" is "FORBIDDEN"

  Scenario: A consumer cannot list campaigns
    When consumer 2001 lists the campaigns
    Then the response has status 403
    And the field "code" is "FORBIDDEN"

  Scenario: Campaigns need a token
    When a visitor lists the campaigns
    Then the response has status 401
    And the field "code" is "UNAUTHORIZED"
