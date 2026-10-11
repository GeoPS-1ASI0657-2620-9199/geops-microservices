@US38 @identity
Feature: Verify the business RUC against SUNAT
  As a business owner, I want to validate my business RUC
  so that I get the verified seal and appear first in the results.

  Background:
    Given the SUNAT registry sample is loaded
    And business owner with business 84 registered RUC "20123456786" at "Av. Larco 345, Miraflores"

  Scenario: An active and habido RUC with a geocodable address is verified with its date
    Given Nominatim finds "Av. Larco 345, Miraflores" at latitude -12.1211 and longitude -77.0297
    When the owner of business 84 requests the verification
    Then the response has status 201
    And the field "result" is "APPROVED"
    And business 84 is VERIFIED with the verification date stored
    And one BusinessVerified event with verified true is stored in the outbox

  Scenario Outline: A RUC that does not exist or is not active is rejected with the reason
    Given business owner with business 85 registered RUC "<ruc>"
    When the owner of business 85 requests the verification
    Then the field "result" is "REJECTED"
    And the field "reason" is "<reason>"
    And business 85 has no verified seal

    Examples:
      | ruc         | reason              |
      | 20999999990 | RUC_NOT_FOUND       |
      | 20111111112 | TAXPAYER_NOT_ACTIVE |
      | 20222222223 | TAXPAYER_NOT_HABIDO |

  Scenario: An address that cannot be geocoded asks for a correction before the seal
    Given Nominatim finds nothing for "Av. Larco 345, Miraflores"
    When the owner of business 84 requests the verification
    Then the field "result" is "REJECTED"
    And the field "reason" is "ADDRESS_NOT_GEOCODABLE"
    And the field "message" is "No ubicamos \"Av. Larco 345, Miraflores\" en el mapa de Lima y Callao. Corrige la dirección (vía, número y distrito) y vuelve a intentarlo; lo demás ya está validado."
    And business 84 is not VERIFIED

  Scenario: The corrected address completes the verification
    Given Nominatim finds "Av. José Larco 345, Miraflores" at latitude -12.1211 and longitude -77.0297
    When the owner of business 84 requests the verification with address "Av. José Larco 345, Miraflores"
    Then the field "result" is "APPROVED"
    And business 84 keeps the address "Av. José Larco 345, Miraflores"

  Scenario: A corrected address that still cannot be geocoded is kept for the next attempt
    Given Nominatim finds nothing for "Av. Jose Larco 345 Mirflores"
    When the owner of business 84 requests the verification with address "Av. Jose Larco 345 Mirflores"
    Then the field "reason" is "ADDRESS_NOT_GEOCODABLE"
    And the field "address" is "Av. Jose Larco 345 Mirflores"
    And business 84 keeps the address "Av. Jose Larco 345 Mirflores"

  Scenario: Nominatim is down and the seal is not granted
    Given Nominatim answers 503
    When the owner of business 84 requests the verification
    Then the field "result" is "PENDING"
    And the field "reason" is "GEOCODER_UNAVAILABLE"
    And the field "verificationStatus" is "PENDING"
    And no BusinessVerified event is stored

  Scenario: A verified business loses the seal when its RUC is no longer active
    Given business 84 is VERIFIED
    And the SUNAT registry now lists RUC "20123456786" as "BAJA DE OFICIO"
    When the owner of business 84 requests the verification
    Then the field "result" is "REJECTED"
    And one BusinessVerified event with verified false is stored in the outbox

  Scenario: Only the owner can request the verification
    When the owner of business 99 requests the verification of business 84
    Then the response has status 403
    And the field "code" is "FORBIDDEN"

  Scenario: The panel reads the status and the last attempt
    Given Nominatim finds "Av. Larco 345, Miraflores" at latitude -12.1211 and longitude -77.0297
    And the owner of business 84 requests the verification
    When the owner of business 84 reads the profile
    Then the response has status 200
    And the field "verificationStatus" is "VERIFIED"
    And the field "lastVerification.result" is "APPROVED"

  Scenario: An empty address is rejected before any check
    When the owner of business 84 requests the verification with address "   "
    Then the response has status 400
    And the field "code" is "INVALID_REQUEST"
