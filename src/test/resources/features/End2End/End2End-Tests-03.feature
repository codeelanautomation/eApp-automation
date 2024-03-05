Feature: FireLight_End2End_Tests

  This feature will verify UAT Application End2End Tests

  @End2EndTest3
  Scenario: TC_01_Complete the REACT application with Pre Signature Review with Signature Then Submit
    Given User is on FireLight login page for TestCase "End2End_SGB"
    Then User on Login Page enters valid username as "Ashwani_FBW" and password and clicks Login button
    Then User clicks "Application" Tab
    Then User selects Jurisdiction "Alabama"
    Then User selects Product Type Dropdown "Variable Annuity"
    Then User opens Given Product "Variable Annuity" for application
    Then User clicks "Create" button
    Then User enters new Application name
    Then User clicks on Create button on Rename window
    Then User verifies Page heading "Annuity Owner Module" with form name "Client Data" for data entry flow
    Then User validate wizard fields for workbook "Template.xlsx"