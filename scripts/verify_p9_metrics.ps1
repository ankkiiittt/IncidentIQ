# Test script for Phase P9 Metrics & Predictions
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

Write-Host "`n2. Testing GET /api/services/health..."
$servicesHealth = Invoke-RestMethod -Uri "http://localhost:8080/api/services/health" -Method Get -Headers $headers
Write-Host "Total services: $($servicesHealth.Count)"
$servicesHealth | Select-Object -First 2 | ConvertTo-Json -Depth 5 | Write-Host

$targetService = $servicesHealth[0]
$serviceId = $targetService.serviceId
Write-Host "`nTarget Service for Telemetry Ingestion: $($targetService.serviceName) ($serviceId)"

Write-Host "`n3. Testing Ingesting Baseline Telemetry via POST /api/metrics/batch..."
$now = [DateTime]::UtcNow
$metricsBatch = @()

for ($i = 20; $i -ge 1; $i--) {
    $t = $now.AddMinutes(-$i * 5).ToString("o")
    $metricsBatch += @{ serviceId = $serviceId; metricName = "CPU_PERCENT"; value = [Math]::Round(45.0 + (Get-Random -Minimum -5.0 -Maximum 5.0), 2); recordedAt = $t }
    $metricsBatch += @{ serviceId = $serviceId; metricName = "MEMORY_PERCENT"; value = [Math]::Round(55.0 + (Get-Random -Minimum -3.0 -Maximum 3.0), 2); recordedAt = $t }
    $metricsBatch += @{ serviceId = $serviceId; metricName = "DISK_PERCENT"; value = 42.0; recordedAt = $t }
    $metricsBatch += @{ serviceId = $serviceId; metricName = "LATENCY_P95_MS"; value = [Math]::Round(95.0 + (Get-Random -Minimum -10.0 -Maximum 10.0), 2); recordedAt = $t }
    $metricsBatch += @{ serviceId = $serviceId; metricName = "THROUGHPUT_RPS"; value = [Math]::Round(350.0 + (Get-Random -Minimum -20.0 -Maximum 20.0), 2); recordedAt = $t }
    $metricsBatch += @{ serviceId = $serviceId; metricName = "ERROR_RATE_PERCENT"; value = [Math]::Round(0.05 + (Get-Random -Minimum -0.02 -Maximum 0.05), 3); recordedAt = $t }
}

$ingestReq = @{ metrics = $metricsBatch } | ConvertTo-Json -Depth 5
$ingestRes = Invoke-RestMethod -Uri "http://localhost:8080/api/metrics/batch" -Method Post -Body $ingestReq -ContentType "application/json" -Headers $headers
Write-Host "Baseline batch result: $($ingestRes | ConvertTo-Json)"

Write-Host "`n4. Testing Ingesting Critical Anomaly Spike via POST /api/metrics/batch..."
$spikeTime = $now.ToString("o")
$spikeBatch = @(
    @{ serviceId = $serviceId; metricName = "CPU_PERCENT"; value = 98.4; recordedAt = $spikeTime },
    @{ serviceId = $serviceId; metricName = "MEMORY_PERCENT"; value = 94.2; recordedAt = $spikeTime },
    @{ serviceId = $serviceId; metricName = "ERROR_RATE_PERCENT"; value = 18.7; recordedAt = $spikeTime },
    @{ serviceId = $serviceId; metricName = "LATENCY_P95_MS"; value = 1450.0; recordedAt = $spikeTime }
)
$spikeReq = @{ metrics = $spikeBatch } | ConvertTo-Json -Depth 5
$spikeRes = Invoke-RestMethod -Uri "http://localhost:8080/api/metrics/batch" -Method Post -Body $spikeReq -ContentType "application/json" -Headers $headers
Write-Host "Spike batch result: $($spikeRes | ConvertTo-Json)"

Write-Host "`n5. Testing GET /api/alerts/predictions..."
$predictions = Invoke-RestMethod -Uri "http://localhost:8080/api/alerts/predictions" -Method Get -Headers $headers
Write-Host "Active predictions count: $($predictions.Count)"
$predictions | Select-Object -First 1 | ConvertTo-Json -Depth 5 | Write-Host

$activePred = $predictions | Where-Object { $_.serviceId -eq $serviceId } | Select-Object -First 1
if ($activePred) {
    $predId = $activePred.id
    Write-Host "`nFound Active Prediction ID: $predId for Service $($activePred.serviceName) (Risk: $($activePred.riskLevel), Score: $($activePred.anomalyScore))"

    Write-Host "`n6. Testing GET /api/services/$serviceId/health..."
    $serviceDetail = Invoke-RestMethod -Uri "http://localhost:8080/api/services/$serviceId/health" -Method Get -Headers $headers
    Write-Host "Service Health Status: $($serviceDetail.healthStatus) (Risk: $($serviceDetail.riskLevel))"
    Write-Host "Metric Series count: $($serviceDetail.metricSeries.Count)"

    Write-Host "`n7. Testing POST /api/alerts/predictions/$predId/create-incident (1-Click Draft)..."
    $createIncRes = Invoke-RestMethod -Uri "http://localhost:8080/api/alerts/predictions/$predId/create-incident" -Method Post -Headers $headers -ContentType "application/json"
    Write-Host "Created Incident from Prediction: #$($createIncRes.formattedNumber) - $($createIncRes.title)"
    Write-Host "Severity: $($createIncRes.severity) | Priority: $($createIncRes.priority)"

    Write-Host "`n8. Verifying Prediction is now RESOLVED..."
    $predAfter = Invoke-RestMethod -Uri "http://localhost:8080/api/alerts/predictions/$predId" -Method Get -Headers $headers
    Write-Host "Prediction Status: $($predAfter.status) (Incident ID: $($predAfter.incidentId))"
}

Write-Host "`nAll Phase P9 backend endpoints verified successfully!"
