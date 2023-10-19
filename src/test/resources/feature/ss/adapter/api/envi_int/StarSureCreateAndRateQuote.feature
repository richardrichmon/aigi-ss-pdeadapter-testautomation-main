Feature: Create, Rate and Issue BOP StartSure

  Background:
    Given run API scenarios on environment "Int"

  @regression
  Scenario Outline: API Validation : <APIName> - "<ScenarioName>"
    Given User setup API configuration for "<APIName>" API

    # Create and Rate Quote for StartSure
    When Read JSON payload file "carq_ss.json" in folder "<testDataFolder>"
    And Replace JSON attribute "{{stateCd}}" with "<state_cd>" and store in payload key name "carq_ss_finalPayload"
    When User hits "POST" api endpoint "createAndRateQuote" with payload key name "carq_ss_finalPayload"
    And User should validate that response body contain values "SUCCESS" for keys "returnCode"
    And User should store value of response path "returnReasonCode" with key "san"
    Then User should get response code as 200

    # Convert to issue
    When Read JSON payload file "convertToIssue.json" in folder "<testDataFolder>"
    And Replace JSON attribute "{{san}}" with "key_san" and store in payload key name "ss_convertToIssue_finalPayload"
    When User hits "POST" api endpoint "convertToIssue" with payload key name "ss_convertToIssue_finalPayload"
    Then User should get response code as 200
    And User should validate that response body contain values "SUCCESS" for keys "returnCode"

    # Slice san to a 10-digit policy number
    And SAN is sliced into 10 character - policyNum

    # Issue Policy with Payment for StartSure
    When Read JSON payload file "issuePolicyWithPayment_ss.json" in folder "<testDataFolder>"
    And Replace JSON attribute "{{policyEffectiveDate}}" with "12/01/2023" and store in payload key name "ss_issue_finalPayload"
    And Replace JSON attribute "{{san}}" with "key_san" and store in payload key name "ss_issue_finalPayload"
    And Replace JSON attribute "{{policyNumber}}" with "key_policyNum" and store in payload key name "ss_issue_finalPayload"
    When User hits "POST" api endpoint "issuePolicyWithPayment" with payload key name "ss_issue_finalPayload"
    Then User should get response code as 200
    And User should validate that response body contain values "SUCCESS" for keys "returnCode"


    Examples:
      | ScenarioName                | APIName             |  testDataFolder   | state_cd |
      | CreateAndRateQuote SS NY    | PDEADAPTER_PEAPI_V2 |  carq_ss_payloads | NY       |
      | CreateAndRateQuote SS AL    | PDEADAPTER_PEAPI_V2 |  carq_ss_payloads | AL       |