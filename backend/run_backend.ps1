# Load database credentials from Windows User Environment
$env:DB_PASSWORD = [System.Environment]::GetEnvironmentVariable('DB_PASSWORD', [System.EnvironmentVariableTarget]::User)
$env:DB_USERNAME = [System.Environment]::GetEnvironmentVariable('DB_USERNAME', [System.EnvironmentVariableTarget]::User)
if (-not $env:DB_USERNAME) { $env:DB_USERNAME = "root" }

Write-Host "Loaded DB_USERNAME: $env:DB_USERNAME"
Write-Host "Starting Spring Boot on http://localhost:8080..."

mvn spring-boot:run
