@US38 @identity
Feature: Load the SUNAT reduced registry
  Identity keeps its own copy of the registry for Lima and Callao, so the verification does not depend on SUNAT
  answering at that moment.

  Scenario: The sample keeps the taxpayers of Lima and Callao with every status
    When the SUNAT registry sample is loaded
    Then the registry has 10 taxpayers
    And the registry lists RUC "20111111111" as "BAJA DE OFICIO" and "HABIDO"
    And the registry lists RUC "20222222222" as "ACTIVO" and "NO HABIDO"
    And the registry does not list RUC "20333333333"

  Scenario: A failed download keeps the previous registry
    Given the SUNAT registry sample is loaded
    When the download of the SUNAT registry fails
    Then the load is reported as failed
    And the registry has 10 taxpayers
