@US25 @identity
Feature: Give the identity service its own schema
  As the technical owner, I want the identity service to work with its own data schema and without foreign keys
  to other contexts so that identity can be deployed and evolve without dragging the rest of the system.

  Scenario: The identity tables declare no foreign key to tables of other contexts
    When the foreign keys of the identity schema are listed
    Then every foreign key points to a table of the identity schema
    And the identity schema has no table of another context

  Scenario: Identity refers to other contexts only by identifier
    When the import graph of Identity is analyzed
    Then no class of Identity depends on another context

  Scenario: Identity reconnects when its database drops the connections
    Given they register as "CONSUMER" with name "Lucía Fernández Ríos", email "lucia.fernandez@ejemplo.pe", phone "987123456" and password "Ofertas#2026"
    When the database closes every connection of Identity
    And they log in with "lucia.fernandez@ejemplo.pe" and "Ofertas#2026"
    Then the response has status 200
    And the account "lucia.fernandez@ejemplo.pe" has a consumer profile
