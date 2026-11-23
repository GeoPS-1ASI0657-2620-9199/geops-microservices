@search-text @catalog
Feature: Search nearby offers by text
  As a consumer, I want to type what I look for in the search bar so that I see only the nearby offers
  whose title, business or category match it, counted and paged by the server.

  Background:
    Given the search origin is latitude -12.1211 and longitude -77.0297
    And these valid offers exist around the origin, each from its own business:
      | title              | meters north | category                   | business             |
      | Café pasado 2x1    | 150          | Cafetería y panadería      | La Esquina           |
      | Menú ejecutivo     | 250          | Gastronomía                | Restaurante Don Pepe |
      | Corte de cabello   | 350          | Belleza y cuidado personal | Barbería Clásica     |
      | Pan con chicharrón | 450          | Cafetería y panadería      | Panadería Santa Rosa |

  Scenario: The text matches the title without caring about case or accents
    When I search within 10 minutes for "CAFE pasado"
    Then the response has status 200
    And the titles are "Café pasado 2x1" in that order
    And totalElements is 1

  Scenario: The text matches the business name and the category
    When I search within 10 minutes for "panaderia"
    Then the titles are "Café pasado 2x1, Pan con chicharrón" in that order
    And totalElements is 2
    When I search within 10 minutes for "don pepe"
    Then the titles are "Menú ejecutivo" in that order

  Scenario: A text without matches answers an empty list
    When I search within 10 minutes for "ferretería"
    Then the response has status 200
    And the result is empty

  Scenario: A text of one character is rejected
    When I search within 10 minutes for "a"
    Then the response has status 400
    And the field "code" is "INVALID_REQUEST"
