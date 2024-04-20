Feature: FILI_Wizard_Tests

  This feature will verify FILI Specification Wizard Validations

  @E2EWizardTestFlow
  Scenario: TC_01_Complete the REACT application with Pre Signature Review with Signature Then Submit
    Given User is on FireLight login page for TestCase "End2End_FILI"
    Then User on Login Page enters valid username as "FILI_FBW" and password and clicks Login button
    Then User clicks application for Product "Variable Annuity" and Product Type "Variable Annuity" and validate wizard fields for workbook "FILI.xlsx"