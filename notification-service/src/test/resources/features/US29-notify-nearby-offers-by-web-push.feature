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

  Scenario: The bell lists the sent notices newest first and leaves out the ones GeoPS did not send
    Given consumer 2001 has a SENT NEARBY_OFFER notice "Oferta vieja" for offer "30001" sent at "2026-11-23T10:00:00Z"
    And consumer 2001 has a PENDING FOLLOWED_BUSINESS_OFFER notice "Oferta nueva" for offer "30002" sent at "2026-11-23T14:00:00Z"
    And consumer 2001 has a SKIPPED_BY_LIMIT NEARBY_OFFER notice "Oferta omitida" for offer "30003" sent at "2026-11-23T15:00:00Z"
    And consumer 2001 has a CANCELLED NEARBY_OFFER notice "Oferta cancelada" for offer "30004" sent at "2026-11-23T16:00:00Z"
    And consumer 2001 has a FAILED NEARBY_OFFER notice "Oferta fallida" for offer "30005" sent at "2026-11-23T17:00:00Z"
    When consumer 2001 lists the notices
    Then the response has status 200
    And the notice list has 2 items
    And notice 1 of the list is "Oferta nueva"
    And notice 1 of the list has offerId "30002"
    And notice 2 of the list is "Oferta vieja"
    And notice 1 of the list is read false

  Scenario: Reading the notices clears the unread count and does not touch another user
    Given consumer 2002 has the recipient copy "otro@example.test" with a confirmed email
    And consumer 2001 has a SENT NEARBY_OFFER notice "Para 2001" for offer "30001" sent at "2026-11-23T10:00:00Z"
    And consumer 2002 has a SENT NEARBY_OFFER notice "Para 2002" for offer "30002" sent at "2026-11-23T11:00:00Z"
    When consumer 2001 reads the unread count
    Then the unread count is 1
    When consumer 2001 marks the notices as read
    Then the response has status 204
    When consumer 2001 reads the unread count
    Then the unread count is 0
    When consumer 2002 reads the unread count
    Then the unread count is 1
    When consumer 2001 lists the notices
    Then notice 1 of the list is read true

  Scenario Outline: The notice list rejects a size outside 1 to 50
    When consumer 2001 lists the notices with size <size>
    Then the response has status 400
    And the field "code" is "INVALID_REQUEST"

    Examples:
      | size |
      | 0    |
      | 51   |

  Scenario: The notice list requires a token and also serves business owners
    When the notice list is requested without a token
    Then the response has status 401
    When a business owner lists the notices
    Then the response has status 200
