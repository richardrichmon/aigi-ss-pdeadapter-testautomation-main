Feature: Document API validation

  Background:
    Given run API scenarios on environment "Int"

  @regression
  Scenario Outline: API Validation : <APIName> - "<ScenarioName>"
    Given User setup API configuration for "<APIName>" API
    When User hits "<Verb>" api endpoint "<endPoint>"
    Then User should get response code as <expResponseCode>

    Examples:
      | ScenarioName                     | APIName             | Verb | endPoint                  | expResponseCode |
      | PDE GET POLICY ENDPOINT          | PDEADAPTER_PEAPI_V2 | GET  | 56000300000000/policy/0   | 200             |