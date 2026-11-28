@US18 @platform
Feature: Document the interfaces of the services
  As the technical owner, I want each service to publish the documentation of its interface
  so that I can integrate and verify each service without reading its code.

  Scenario Outline: The service publishes its OpenAPI through the gateway
    When the OpenAPI document of "<service>" is requested at /docs/<service>/v3/api-docs
    Then the response has status 200
    And the document lists its operations with their parameters and responses

    Examples:
      | service      |
      | identity     |
      | catalog      |
      | reservation  |
      | engagement   |
      | notification |

  Scenario Outline: The Swagger UI of the service answers
    When the Swagger UI of "<service>" is requested at /docs/<service>/swagger-ui.html
    Then the response has status 200

    Examples:
      | service      |
      | identity     |
      | catalog      |
      | reservation  |
      | engagement   |
      | notification |

  Scenario Outline: The document brings examples
    When the OpenAPI document of "<service>" is requested at /docs/<service>/v3/api-docs
    Then the document has at least one example

    Examples:
      | service      |
      | identity     |
      | catalog      |
      | reservation  |
      | engagement   |
      | notification |

  Scenario: The documentation needs no token
    When an anonymous user requests the OpenAPI document of each service through the gateway
    Then every one answers 200
