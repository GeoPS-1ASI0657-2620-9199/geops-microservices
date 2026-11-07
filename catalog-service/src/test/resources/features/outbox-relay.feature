@US33 @catalog @outbox
Feature: Publish the Catalog outbox to Kafka
  Publishing a campaign stores one OfferPublished per offer in the same transaction;
  a relay publishes them to catalog.events, and the campaign does not depend on Kafka being up.

  Background:
    Given business 84 "Restaurante Don Pepe" is known to Catalog
    And the current date in Lima is "2026-10-27"

  Scenario: A published campaign reaches the catalog.events topic
    When business owner of business 84 publishes a campaign with offers "2x1 en almuerzos ejecutivos" and "Postre gratis con el menú"
    Then the response has status 201
    And 2 OfferPublished events of the campaign arrive at topic "catalog.events" with the campaign id as key

  Scenario: The campaign is published while Kafka is stopped and the events leave when Kafka is back
    Given Kafka is stopped
    When business owner of business 84 publishes a campaign with offer "2x1 en almuerzos ejecutivos"
    Then the response has status 201
    And after 8 seconds the outbox rows of the campaign are still unpublished
    When Kafka is back
    Then 1 OfferPublished event of the campaign arrives at topic "catalog.events" with the campaign id as key
