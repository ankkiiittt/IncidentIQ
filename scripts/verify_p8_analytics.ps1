# Test script for Phase P8 Analytics Endpoints
$loginBody = @{
    email = "admin@incidentiq.local"
    password = "AdminInitialPassword123!"
} | ConvertTo-Json

Write-Host "1. Logging in as Admin..."
$loginRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $loginRes.accessToken
Write-Host "Logged in successfully as $($loginRes.user.name). Token received."

$headers = @{
    Authorization = "Bearer $token"
}

Write-Host "`n2. Testing GET /api/analytics/overview?days=30..."
$overview = Invoke-RestMethod -Uri "http://localhost:8080/api/analytics/overview?days=30" -Method Get -Headers $headers
$overview | ConvertTo-Json -Depth 5 | Write-Host

Write-Host "`n3a. Testing GET /api/analytics/volume?days=30&groupBy=day..."
$volume = Invoke-RestMethod -Uri "http://localhost:8080/api/analytics/volume?days=30&groupBy=day" -Method Get -Headers $headers
Write-Host "Daily Points Count: $($volume.points.Length)"

Write-Host "`n3b. Testing GET /api/analytics/volume?days=30&groupBy=week..."
$volumeWeekly = Invoke-RestMethod -Uri "http://localhost:8080/api/analytics/volume?days=30&groupBy=week" -Method Get -Headers $headers
$volumeWeekly | ConvertTo-Json -Depth 5 | Write-Host

Write-Host "`n4. Testing GET /api/analytics/sla?days=30..."
$sla = Invoke-RestMethod -Uri "http://localhost:8080/api/analytics/sla?days=30" -Method Get -Headers $headers
$sla | ConvertTo-Json -Depth 5 | Write-Host

Write-Host "`n5. Testing GET /api/analytics/causes?days=30..."
$causes = Invoke-RestMethod -Uri "http://localhost:8080/api/analytics/causes?days=30" -Method Get -Headers $headers
$causes | ConvertTo-Json -Depth 5 | Write-Host

Write-Host "`n6. Testing GET /api/analytics/ai-feedback?days=30..."
$aiFeedback = Invoke-RestMethod -Uri "http://localhost:8080/api/analytics/ai-feedback?days=30" -Method Get -Headers $headers
$aiFeedback | ConvertTo-Json -Depth 5 | Write-Host

Write-Host "`nAll 5 endpoints responded successfully!"
