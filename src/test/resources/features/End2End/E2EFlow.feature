Feature: Client_E2E_Tests

  This feature will verify Client Specification E2E Validations

  @ModuleTag
  Scenario: TC_E2E test for Client for ModuleName and jurisdiction state
    Given User is on FireLight login page for TestCase "End2EndFlow_Client-ModuleTag-state_Client"
    Then User on Login Page enters valid username as "Client_User" and password and clicks Login button
    Then User clicks application for Product "productName" and Product Type "productName" and validate wizard fields for module "ModuleName" and jurisdiction "state"