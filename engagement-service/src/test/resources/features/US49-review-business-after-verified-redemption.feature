@US49 @GEO-209 @engagement
Feature: Review a business after a verified redemption
  As a consumer, I want to leave my opinion after using an offer so that others know how the real experience was.

  Background:
    Given business 1 is called "Bodega Doña Rosa"

  Scenario: A consumer who redeemed a reservation publishes a verified review
    Given consumer 1 redeemed reservation 1 at business 1
    When consumer 1 reviews business 1 with 5 stars and the text "Respetaron el precio"
    Then the response has status 201
    And the field "verifiedRedemption" is "true"
    And the field "rating" is "5"
    And the field "businessId" is "1"
    And 1 review is stored for business 1

  Scenario: A consumer without a redemption in that business cannot review
    When consumer 2 reviews business 1 with 4 stars and the text "Buena atención"
    Then the response has status 403
    And the field "code" is "REDEMPTION_REQUIRED"
    And no review was stored

  Scenario: A second review for the same single redemption is rejected
    Given consumer 1 redeemed reservation 1 at business 1
    And consumer 1 already reviewed business 1
    When consumer 1 reviews business 1 with 5 stars and the text "Volví y otra vez bien"
    Then the response has status 409
    And the field "code" is "REVIEW_ALREADY_EXISTS"
    And 1 review is stored for business 1

  Scenario: Each redemption allows its own review
    Given consumer 1 redeemed reservation 1 at business 1
    And consumer 1 redeemed reservation 2 at business 1
    And consumer 1 already reviewed business 1
    When consumer 1 reviews business 1 with 5 stars and the text "Volví y otra vez bien"
    Then the response has status 201
    And 2 reviews are stored for business 1

  Scenario Outline: A rating outside 1 to 5 is rejected
    Given consumer 1 redeemed reservation 1 at business 1
    When consumer 1 reviews business 1 with <stars> stars and the text "Respetaron el precio"
    Then the response has status 400
    And the field "code" is "INVALID_REQUEST"
    And no review was stored

    Examples:
      | stars |
      | 0     |
      | 6     |

  Scenario: The reviews of a business are listed newest first
    Given consumer 1 redeemed reservation 1 at business 1
    And consumer 2 redeemed reservation 2 at business 1
    And consumer 1 reviews business 1 with 5 stars and the text "Primero"
    And consumer 2 reviews business 1 with 4 stars and the text "Segundo"
    When consumer 3 lists the reviews of business 1
    Then the response has status 200
    And the reviews come in this order: "Segundo, Primero"

  Scenario Outline: Only a consumer can publish a review
    Given consumer 1 redeemed reservation 1 at business 1
    When business 1 is reviewed <token> with 5 stars
    Then the response has status <status>
    And the field "code" is "<code>"
    And no review was stored

    Examples:
      | token                       | status | code         |
      | without a token             | 401    | UNAUTHORIZED |
      | with a business owner token | 403    | FORBIDDEN    |
