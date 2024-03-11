chmod +x allure-report.sh
cd \target
allure generate --single-file --clean allure-results

# Generate Allure report
#allure generate --clean

# Open Allure report in browser
#allure open