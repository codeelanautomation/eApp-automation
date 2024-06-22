Feature: This feature will verify Form data with inbound XML

  @InboundFlow
  Scenario:Verify Form data with inbound and outbound Xml of Client-1
    Given User is on "Dev" Test page for TestCase "End2End_InboundFlow_Client"
    Then Import client XML file in app
    Then Verify data on UI is populated as given in inbound XML and validate rules for "All" modules
    Then User Extracts the URL Link for "Client" and Stores it
    Then User completes the application

  @OutboundFlow
  Scenario:Verify Form data with outbound Xml of Client-2
    Given User is on "Firelight" Test page for TestCase "End2End_OutboundFlow_Client"
    Then User on Login Page enters valid username as "Client_User" and password and clicks Login button
    Then User open application from recent activity
    Then Verify data on outbound XML is in sync with data on UI for "All" modules
