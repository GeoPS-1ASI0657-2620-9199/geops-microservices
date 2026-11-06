@US29 @notification
Feature: Notify nearby offers by web push
  As a consumer I receive relevant browser notices after granting permission.

  Background:
    Given consumer 2001 allowed push with a browser subscription

  Scenario: A recent location inside the radius receives a notice
    Given consumer 2001 was nearby 20 minutes ago with radius 800 meters
    When Catalog publishes the same nearby push offer 1 times
    Then the consumer receives 1 recorded web push

  Scenario: A location older than 60 minutes receives no proximity notice
    Given consumer 2001 was nearby 61 minutes ago with radius 800 meters
    When Catalog publishes the same nearby push offer 1 times
    Then the consumer receives 0 recorded web push

  Scenario: An offer outside the chosen radius sends nothing
    Given consumer 2001 was nearby 20 minutes ago with radius 100 meters
    When Catalog publishes the same nearby push offer 1 times
    Then the consumer receives 0 recorded web push

  Scenario: A followed business notifies without a recent location
    Given consumer 2001 follows the publishing business
    When Catalog publishes the same nearby push offer 1 times
    Then the consumer receives 1 recorded web push

  Scenario: Repeated delivery of the same offer notifies only once
    Given consumer 2001 was nearby 20 minutes ago with radius 800 meters
    When Catalog publishes the same nearby push offer 2 times
    Then the consumer receives 1 recorded web push
