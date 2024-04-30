Feature: FILI_Wizard_Tests

  This feature will verify FILI Specification Wizard Validations

  @Module
  Scenario: TC_01_Complete the REACT application with Pre Signature Review with Signature Then Submit
    Given User is on FireLight login page for TestCase "End2End_Client"
    Then User on Login Page enters valid username as "Client_User" and password and clicks Login button
    Then User clicks application for Product "productName" and Product Type "productName" and validate wizard fields for module "ModuleName" for workbook "fileName"
