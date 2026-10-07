@GEO-9 @GEO-51 @catalog
Feature: Versioned schema migrations
  As the technical lead I want the schema to change only through versioned migrations
  so that it can be reproduced on any database and never generated automatically.

  Scenario: An empty database gets the schema the service expects
    Given an empty database with PostGIS
    When the migrations run in order
    Then every pending migration is applied successfully
    And every table and column the service maps exists in the resulting schema

  Scenario: Running the migrations again changes nothing
    Given an empty database with PostGIS
    And the migrations already ran
    When the migrations run again
    Then no migration is applied
    And the schema is the same as before the second run

  Scenario: The service never generates its schema
    Then Hibernate only validates the schema
    And every migration file is a versioned migration
