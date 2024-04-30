Feature: Client_Wizard_Tests

  This feature will verify Client Specification Wizard Validations

  @ModuleTag
  Scenario: TC_Wizard test for Client for ModuleName
    Given User is on FireLight login page for TestCase "End2End_Client-ModuleTag_Client"
    Then User on Login Page enters valid username as "Client_User" and password and clicks Login button
    Then User clicks application for Product "productName" and Product Type "productName" and validate wizard fields for module "ModuleName" for workbook "fileName"
