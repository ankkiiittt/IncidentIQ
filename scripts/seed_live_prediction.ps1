# Seeds an active prediction for live UI verification
$loginBody = @{
    email = "admin@incidentiq.local"
    password = "AdminInitialPassword123!"
} | ConvertTo-Json

$loginRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$headers = @{ Authorization = "Bearer " + $loginRes.accessToken }

$services = Invoke-RestMethod -Uri "http://localhost:8080/api/services/health" -Method Get -Headers $headers
$sId = $services[0].serviceId
$t = [DateTime]::UtcNow.ToString("o")

$batch = @(
    @{ serviceId = $sId; metricName = "CPU_PERCENT"; value = 98.9; recordedAt = $t },
    @{ serviceId = $sId; metricName = "MEMORY_PERCENT"; value = 96.1; recordedAt = $t },
    @{ serviceId = $sId; metricName = "ERROR_RATE_PERCENT"; value = 16.5; recordedAt = $t },
    @{ serviceId = $sId; metricName = "LATENCY_P95_MS"; value = 1820.0; recordedAt = $t }
)

$res = Invoke-RestMethod -Uri "http://localhost:8080/api/metrics/batch" -Method Post -Body (@{ metrics = $batch } | ConvertTo-Json) -ContentType "application/json" -Headers $headers
Write-Host "Seeded active prediction anomaly: $($res | ConvertTo-Json)"
