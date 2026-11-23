@US28 @notification
Feature: Learn about the accounts from the events of identity
  As the technical owner, I want notifications to learn about the accounts from the events of identity
  so that identity never calls notifications and both can be deployed independently.

  Scenario: A new consumer gets the recipient copy, the default preferences and the confirmation email
    When Identity publishes UserRegistered for consumer 2001 with a confirmation link
    Then consumer 2001 has a recipient copy with the email unconfirmed
    And consumer 2001 has both offer channels off and a limit of 3
    And 1 confirmation email with the link of the account is sent

  Scenario: A repeated UserRegistered does not send a second confirmation email
    Given Identity publishes UserRegistered for consumer 2001 with a confirmation link
    When Identity publishes UserRegistered for consumer 2001 with a confirmation link again
    Then 1 confirmation email with the link of the account is sent

  Scenario: EmailConfirmed marks the recipient copy as confirmed
    Given Identity publishes UserRegistered for consumer 2001 with a confirmation link
    When Identity publishes EmailConfirmed for consumer 2001
    Then consumer 2001 has a recipient copy with the email confirmed

  Scenario: There is no cycle between notifications and identity
    When the import graph of Notification is analyzed
    Then no class of Notification depends on Identity
    And no class of Notification makes an HTTP call to another service
