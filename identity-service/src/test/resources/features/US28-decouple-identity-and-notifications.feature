@US28 @identity
Feature: Decouple identity and notifications
  As the technical owner, I want identity not to call the notification service synchronously
  so that a circular dependency does not stop both from being deployed independently.

  Scenario: The registration publishes an event and does not call Notification
    Given Notification and the event bus are not reachable from Identity
    When they register as "CONSUMER" with name "Lucía Fernández Ríos", email "lucia.fernandez@ejemplo.pe", phone "987123456" and password "Ofertas#2026"
    Then the response has status 201
    And the outbox has the event "UserRegistered" of the account "lucia.fernandez@ejemplo.pe"
    And the event "UserRegistered" carries a confirmation link that is not stored in the database

  Scenario: The confirmation link confirms the email and publishes EmailConfirmed
    Given they register as "CONSUMER" with name "Lucía Fernández Ríos", email "lucia.fernandez@ejemplo.pe", phone "987123456" and password "Ofertas#2026"
    When they open the confirmation link of "lucia.fernandez@ejemplo.pe"
    Then the response has status 204
    And the email of the account "lucia.fernandez@ejemplo.pe" is confirmed
    And the outbox has the event "EmailConfirmed" of the account "lucia.fernandez@ejemplo.pe"

  Scenario: A confirmation link works only once
    Given they register as "CONSUMER" with name "Lucía Fernández Ríos", email "lucia.fernandez@ejemplo.pe", phone "987123456" and password "Ofertas#2026"
    And they open the confirmation link of "lucia.fernandez@ejemplo.pe"
    When they open the confirmation link of "lucia.fernandez@ejemplo.pe"
    Then the response has status 400
    And the field "code" is "INVALID_CONFIRMATION_LINK"
    And the outbox has 1 event "EmailConfirmed" of the account "lucia.fernandez@ejemplo.pe"

  Scenario: There is no cycle between identity and notifications
    When the import graph of Identity is analyzed
    Then no class of Identity depends on Notification
    And only the adapters of the external providers make HTTP calls
