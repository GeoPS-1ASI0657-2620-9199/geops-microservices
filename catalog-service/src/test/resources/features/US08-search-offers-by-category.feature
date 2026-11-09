@US08 @catalog
Feature: Search offers by category
  As a consumer, I want to filter offers by category so that I find the kind of business I look for.

  Background:
    Given the search origin is latitude -12.1211 and longitude -77.0297

  Scenario: Only offers of the selected category are listed
    Given these valid offers exist around the origin, each from its own business:
      | title            | meters north | category                   |
      | Menú ejecutivo   | 200          | Gastronomía                |
      | Corte de cabello | 300          | Belleza y cuidado personal |
      | Ceviche 2x1      | 500          | Gastronomía                |
    When I search within 10 minutes with category "Gastronomía"
    Then the titles are "Menú ejecutivo, Ceviche 2x1" in that order

  Scenario: A category without offers returns an empty list and nothing else
    Given these valid offers exist around the origin, each from its own business:
      | title            | meters north | category                   |
      | Menú ejecutivo   | 200          | Gastronomía                |
      | Corte de cabello | 300          | Belleza y cuidado personal |
    When I search within 10 minutes with category "Mascotas"
    Then the response has status 200
    And the result is empty

  Scenario: An unknown category is rejected with the valid list
    When I search within 10 minutes with category "Juguetería"
    Then the response has status 400
    And the field "code" is "INVALID_CATEGORY"

  Scenario: The filter covers the whole radius before paging
    Given 30 valid offers exist around the origin and only number 25 is of category "Mascotas"
    When I search within 10 minutes with category "Mascotas"
    Then the first page has "Oferta 25"
