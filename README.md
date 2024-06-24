How to execute locally

applicationType=app
environment=qanext
execution.type=local
execution.mode=local

1. Open FlowInterface.xlsx and update Client, Product and other required details.
2. If separate report for all the modules is required, set Execute for required modules to Yes. Set No to "All".
3. If single report for all the modules is required, set Execute for "All" only to Yes.
4. If report for any module is needed as per Jurisdiction, set corresponding JurisdcitionWiseReport column to Yes.
5. Run command : mvn clean test -Dtest="**/RunInterfaceTest.class/**"
   It will generate input JSON along with required feature/runner files.
6. Run command : mvn clean test
7. Once executed, check the cucumber report in path -> target/cucumber-report.
8. Publishing Cucumber Reports on Cloud forever
   a) Run cmd as Administrator -  setx /M CUCUMBER_PUBLISH_TOKEN "token code"
   b) Restart your machine