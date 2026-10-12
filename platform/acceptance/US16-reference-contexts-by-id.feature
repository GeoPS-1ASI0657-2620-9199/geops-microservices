@US16 @platform
Feature: Reference other contexts only by identifier
  As the technical owner, I want no aggregate to declare an aggregate of another context as a field
  so that there are no foreign keys between the databases of the services.

  Scenario: Every aggregate refers to other contexts by identifier
    When the ArchUnit context boundary test runs in the five services
    Then it passes in all of them

  Scenario: There is no integrity constraint between tables of different services
    When the foreign keys of the five databases are listed
    Then every foreign key points to a table of its own database
    And catalog_user cannot connect to identity_db

  Scenario: Data of another context comes from its published operation
    Given Catalog has offer 1052 valid until tomorrow
    When consumer 2001 reserves offer 1052
    Then Reservation asked Catalog through GET /internal/v1/offers/1052/availability
    And reservation_db has no table of catalog_db
