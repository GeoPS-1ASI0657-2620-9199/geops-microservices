@US25 @platform
Feature: Give the identity service its own schema
  As the technical owner, I want the identity service to work with its own data schema and without foreign keys
  to other contexts so that identity can be deployed and evolve without dragging the rest of the system.

  Scenario: The identity tables declare no foreign key to tables of other contexts
    When the foreign keys of the five databases are listed
    Then every foreign key of identity_db points to a table of identity_db
    And no service user can connect to another database

  Scenario: Other contexts refer to the user by identifier
    When the ArchUnit context boundary test runs in Catalog, Reservation, Engagement and Notification
    Then it passes in all of them

  Scenario: The rest of the system keeps working while identity restarts
    Given a consumer registered before the restart
    When I restart identity-service
    And a visitor searches nearby offers through the gateway during the restart
    Then the search answers 200
    And identity-service is healthy again within 120 seconds
    And the consumer registered before the restart logs in
