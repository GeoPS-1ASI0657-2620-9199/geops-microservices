@US17 @platform
Feature: Deploy the offer catalog as an independent service
  As the technical owner, I want the offer catalog to run as an independent service
  so that the offer search scales without scaling the rest of the system.

  Scenario: Raising the catalog capacity does not redeploy the other services
    Given the platform is running with one catalog-service replica
    And I record the start time of every other container
    When I scale catalog-service to 2 replicas without dependencies
    Then 2 catalog-service replicas are healthy
    And every other container keeps its start time
    And 20 nearby searches through the gateway are answered by both replicas

  Scenario: The catalog answers with its own data only
    When the ArchUnit catalog independence test runs
    Then it passes

  Scenario: Identity keeps working while the catalog restarts
    When I restart catalog-service
    And a consumer logs in during the restart
    Then the login answers 200
