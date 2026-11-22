@US24 @catalog
Feature: Moderate offers from the admin panel
  As the platform administrator, I want to see the offers with their status and remove the ones that break the
  publication rules so that the catalog only shows offers that can be trusted.

  Background:
    Given the search origin is latitude -12.1211 and longitude -77.0297
    And the current date in Lima is "2026-11-22"
    And business 84 has an active campaign "Almuerzos de noviembre" until "2026-11-30" with offer "2x1 en almuerzos ejecutivos" 200 meters north of the origin
    And I searched within 10 minutes once

  Scenario: The administrator sees the offers with their status
    When administrator 1 lists the offers for moderation
    Then the response has status 200
    And the moderation list has offer "2x1 en almuerzos ejecutivos" with status "PUBLISHED" of "Restaurante Don Pepe"

  Scenario: The administrator can keep only the offers of businesses with open reports
    Given business 85 has an active campaign "Cafés de noviembre" until "2026-11-30" with offer "Café americano gratis" 300 meters north of the origin
    And consumers opened 2 reports against business 84
    When administrator 1 lists the reported offers for moderation
    Then the response has status 200
    And the moderation list has only "2x1 en almuerzos ejecutivos"

  Scenario: A removed offer leaves the catalog and the history records who removed it and why
    When administrator 1 removes offer "2x1 en almuerzos ejecutivos" because "La oferta anuncia un precio que el local no respeta."
    Then the response has status 200
    And the field "offerStatus" is "REMOVED"
    And the field "adminId" is "1"
    And the moderation history shows that administrator 1 removed "2x1 en almuerzos ejecutivos" because "La oferta anuncia un precio que el local no respeta."
    And an OfferRemoved event of offer "2x1 en almuerzos ejecutivos" is in the outbox
    When I search within 10 minutes again
    Then the result does not have "2x1 en almuerzos ejecutivos"
    When a consumer opens the detail of offer "2x1 en almuerzos ejecutivos"
    Then the response has status 404

  Scenario: A removed offer cannot be reserved but keeps its record
    Given administrator 1 removed offer "2x1 en almuerzos ejecutivos" because "Imagen tomada de otro comercio"
    When Reservation asks for the availability of offer "2x1 en almuerzos ejecutivos"
    Then the response has status 200
    And the field "available" is "false"
    When administrator 1 lists the removed offers for moderation
    Then the moderation list has offer "2x1 en almuerzos ejecutivos" with status "REMOVED" of "Restaurante Don Pepe"

  Scenario: The reason is mandatory
    When administrator 1 removes offer "2x1 en almuerzos ejecutivos" because ""
    Then the response has status 400
    And the field "code" is "INVALID_REQUEST"
    And the moderation history is empty
    When I search within 10 minutes again
    Then the result has "2x1 en almuerzos ejecutivos"

  Scenario: An offer already removed is not removed twice
    Given administrator 1 removed offer "2x1 en almuerzos ejecutivos" because "Imagen tomada de otro comercio"
    When administrator 2 removes offer "2x1 en almuerzos ejecutivos" because "Precio distinto al del local"
    Then the response has status 409
    And the field "code" is "OFFER_ALREADY_REMOVED"
    And the moderation history has 1 action

  Scenario: An unknown offer cannot be removed
    When administrator 1 removes offer 999999 because "La oferta no cumple las condiciones"
    Then the response has status 404
    And the field "code" is "OFFER_NOT_FOUND"

  Scenario: Without the administrator profile the panel is rejected
    When business owner of business 84 asks for the moderation panel
    Then the response has status 403
    When business owner of business 84 removes offer "2x1 en almuerzos ejecutivos" because "Quiero retirar mi propia oferta"
    Then the response has status 403
    And the moderation history is empty
    When someone without a token asks for the moderation panel
    Then the response has status 401
